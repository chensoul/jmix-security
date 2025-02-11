/*
 * Copyright 2020 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.jmix.localfs;

import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.jmix.core.CoreProperties;
import io.jmix.core.filestore.FileRef;
import io.jmix.core.filestore.FileStorage;
import io.jmix.core.filestore.FileStorageException;
import io.jmix.core.util.UuidProvider;
import jakarta.annotation.PreDestroy;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import static java.nio.file.StandardOpenOption.CREATE_NEW;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component("locfs_FileStorage")
public class LocalFileStorage implements FileStorage {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorage.class);

    public static final String DEFAULT_STORAGE_NAME = "fs";

    protected String storageName;
    protected String storageDir;

    @Autowired
    protected LocalFileStorageProperties properties;

    @Autowired
    protected CoreProperties coreProperties;

    @Value("${jmix.localfs.disable-path-check:false}")
    protected Boolean disablePathCheck;

    protected boolean isImmutableFileStorage;

    protected ExecutorService writeExecutor = Executors.newFixedThreadPool(5,
            new ThreadFactoryBuilder().setNameFormat("FileStorageWriter-%d").build());

    protected volatile Path[] storageRoots;

    public LocalFileStorage() {
        this(DEFAULT_STORAGE_NAME);
    }

    public LocalFileStorage(String storageName) {
        this.storageName = storageName;
    }

    /**
     * Optional constructor that allows specifying storage directory,
     * thus overriding {@link LocalFileStorageProperties#getStorageDir()} property.
     * <p>
     * It can be useful if there are more than one local file storage in an application,
     * and these storages should be using different dirs for storing files.
     */
    public LocalFileStorage(String storageName, String storageDir) {
        this(storageName);
        this.storageDir = storageDir;
    }

    @Override
    public String getStorageName() {
        return storageName;
    }

    protected String createUuidFilename(String fileName) {
        String extension = FilenameUtils.getExtension(fileName);
        if (StringUtils.isNotEmpty(extension)) {
            return UuidProvider.createUuid().toString() + "." + extension;
        } else {
            return UuidProvider.createUuid().toString();
        }
    }

    protected Path[] getStorageRoots() {
        if (storageRoots==null) {
            String storageDir = this.storageDir!=null ? this.storageDir:properties.getStorageDir();
            if (StringUtils.isBlank(storageDir)) {
                String workDir = coreProperties.getWorkDir();
                Path dir = Paths.get(workDir, "filestorage");
                if (!dir.toFile().exists() && !dir.toFile().mkdirs()) {
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,
                            "Cannot create filestorage directory: " + dir.toAbsolutePath().toString());
                }
                storageRoots = new Path[]{dir};
            } else {
                List<Path> list = new ArrayList<>();
                for (String str : storageDir.split(",")) {
                    str = str.trim();
                    if (!StringUtils.isEmpty(str)) {
                        Path path = Paths.get(str);
                        if (!list.contains(path))
                            list.add(path);
                    }
                }
                storageRoots = list.toArray(new Path[0]);
            }
        }
        return storageRoots;
    }

    public long saveStream(FileRef fileRef, InputStream inputStream) {
        Path relativePath = getRelativePath(fileRef.getPath());

        Path[] roots = getStorageRoots();

        // Store to primary storage
        checkStorageDefined(roots, fileRef.getFileName());
        checkPrimaryStorageAccessible(roots, fileRef.getFileName());

        Path path = roots[0].resolve(relativePath);
        Path parentPath = path.getParent();
        if (parentPath==null) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,
                    "Invalid storage root: " + path);
        }
        if (!parentPath.toFile().exists() && !parentPath.toFile().mkdirs()) {
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,
                    "Cannot create directory: " + parentPath.toAbsolutePath());
        }

        checkFileExists(path);

        long size;
        long maxAllowedSize = properties.getMaxFileSize().toBytes();
        try (OutputStream outputStream = Files.newOutputStream(path, CREATE_NEW)) {
            size = IOUtils.copyLarge(inputStream, outputStream, 0, maxAllowedSize);

            if (size >= maxAllowedSize) {
                if (inputStream.read()!=IOUtils.EOF) {
                    outputStream.close();
                    if (path.toFile().exists()) {
                        if (!path.toFile().delete()) {
                            log.warn("Failed to delete an incorrectly uploaded file '{}'. " +
                                            "File was to large and has been rejected but already loaded part was not deleted.",
                                    path.toAbsolutePath());
                        }
                        ;
                    }

                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,
                            String.format("File is too large: '%s'. Max file size = %s MB is exceeded but there are unread bytes left.",
                                    path.toAbsolutePath(),
                                    properties.getMaxFileSize().toMegabytes()));
                }
            }
            outputStream.flush();
//            writeLog(path, false);
        } catch (IOException e) {
            FileUtils.deleteQuietly(path.toFile());
            throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION, path.toAbsolutePath().toString(), e);
        }

        // Copy file to secondary storages asynchronously
        for (int i = 1; i < roots.length; i++) {
            if (!roots[i].toFile().exists()) {
                log.error("Error saving {} into {} : directory doesn't exist", fileRef.getFileName(), roots[i]);
                continue;
            }

            Path pathCopy = roots[i].resolve(relativePath);

            writeExecutor.submit(() -> {
                try {
                    FileUtils.copyFile(path.toFile(), pathCopy.toFile(), true);
                } catch (Exception e) {
                    log.error("Error saving {} into {} : {}", fileRef.getFileName(), pathCopy, e.getMessage());
                }
            });
        }

        return size;
    }

    @Override
    public FileRef saveStream(String fileName, InputStream inputStream, Map<String, Object> parameters) {
        Path relativePath = createRelativeFilePath(fileName);
        Map<String, String> fileRefParams = Maps.toMap(parameters.keySet(), key -> parameters.get(key).toString());
        FileRef fileRef = new FileRef(storageName, pathToString(relativePath), fileName, fileRefParams);
        saveStream(fileRef, inputStream);
        return fileRef;
    }

    protected Path createRelativeFilePath(String fileName) {
        return createDateDirPath().resolve(createUuidFilename(fileName));
    }

    @Override
    public InputStream openStream(FileRef reference) {
        Path relativePath = getRelativePath(reference.getPath());

        Path[] roots = getStorageRoots();
        if (roots.length==0) {
            log.error("No storage directories available");
            throw new FileStorageException(FileStorageException.Type.FILE_NOT_FOUND, reference.toString());
        }

        InputStream inputStream = null;
        for (Path root : roots) {
            Path path = root.resolve(relativePath);

            if (!path.toFile().exists()) {
                log.error("File " + path + " not found");
                continue;
            }

            try {
                if (!Boolean.TRUE.equals(disablePathCheck) && !path.toRealPath().startsWith(root.toRealPath())) {
                    log.error("File '{}' is outside of root dir '{}': ", path, root);
                    continue;
                }

                inputStream = Files.newInputStream(path);
            } catch (IOException e) {
                log.error("Error opening input stream for " + path, e);
            }
        }

        if (inputStream!=null) {
            return inputStream;
        } else {
            throw new FileStorageException(FileStorageException.Type.FILE_NOT_FOUND, reference.toString());
        }
    }

    @Override
    public void removeFile(FileRef reference) {
        Path[] roots = getStorageRoots();
        if (roots.length==0) {
            log.error("No storage directories defined");
            return;
        }

        Path relativePath = getRelativePath(reference.getPath());
        for (Path root : roots) {
            Path filePath = root.resolve(relativePath);
            File file = filePath.toFile();
            if (file.exists()) {
                if (!file.delete()) {
                    throw new FileStorageException(FileStorageException.Type.IO_EXCEPTION,
                            "Unable to delete file " + file.getAbsolutePath());
                }
            }
        }
    }

    @Override
    public boolean fileExists(FileRef reference) {
        Path[] roots = getStorageRoots();

        Path relativePath = getRelativePath(reference.getPath());
        for (Path root : roots) {
            Path filePath = root.resolve(relativePath);
            if (filePath.toFile().exists()) {
                return true;
            }
        }
        return false;
    }

    protected Path createDateDirPath() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(new Date());
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);

        return Paths.get(String.valueOf(year),
                StringUtils.leftPad(String.valueOf(month), 2, '0'),
                StringUtils.leftPad(String.valueOf(day), 2, '0'));
    }

    protected void checkFileExists(Path path) {
        if (Files.exists(path) && isImmutableFileStorage) {
            throw new FileStorageException(FileStorageException.Type.FILE_ALREADY_EXISTS,
                    path.toAbsolutePath().toString());
        }
    }

    protected void checkDirectoryExists(Path dir) {
        if (!dir.toFile().exists())
            throw new FileStorageException(FileStorageException.Type.STORAGE_INACCESSIBLE,
                    dir.toAbsolutePath().toString());
    }

    protected void checkPrimaryStorageAccessible(Path[] roots, String fileName) {
        if (!roots[0].toFile().exists() && !roots[0].toFile().mkdirs()) {
            log.error("Inaccessible primary storage at {}", roots[0]);
            throw new FileStorageException(FileStorageException.Type.STORAGE_INACCESSIBLE, fileName);
        }
    }

    protected void checkStorageDefined(Path[] roots, String fileName) {
        if (roots.length==0) {
            log.error("No storage directories defined");
            throw new FileStorageException(FileStorageException.Type.STORAGE_INACCESSIBLE, fileName);
        }
    }

    /**
     * This method is mostly needed for compatibility with an old API.
     * <p>
     * If {@link #isImmutableFileStorage} is false then {@link #saveStream(FileRef, InputStream)}
     * will be overwriting existing files.
     */
    public void setImmutableFileStorage(boolean immutableFileStorage) {
        isImmutableFileStorage = immutableFileStorage;
    }

    /**
     * Converts string path to {@link Path}.
     */
    protected Path getRelativePath(String path) {
        String[] parts = path.split("/", 4);
        if (parts.length < 4) {
            throw new IllegalArgumentException("Invalid path");
        }
        return Paths.get(parts[0], parts[1], parts[2], parts[3]);
    }

    /**
     * Converts path to a uniform string representation ("yyyy/mm/dd/uuid.ext").
     */
    protected String pathToString(Path path) {
        return path.toString().replace('\\', '/');
    }

    @PreDestroy
    protected void stopWriteExecutor() {
        writeExecutor.shutdown();
    }

}
