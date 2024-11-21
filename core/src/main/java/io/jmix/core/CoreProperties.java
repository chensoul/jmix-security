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

package io.jmix.core;

import io.jmix.core.message.LocaleResolver;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "jmix.core")
public class CoreProperties {
    String confDir;
    String workDir;
    String tempDir;
    String defaultFileStorage;
    List<Locale> availableLocales;

    public CoreProperties(
            String confDir,
            String workDir,
            String tempDir,
            String dbDir,
            List<String> availableLocales,
            String defaultFileStorage) {
        this.confDir = confDir;
        this.workDir = workDir;
        this.tempDir = tempDir;
        this.defaultFileStorage = defaultFileStorage;
        if (availableLocales==null) {
            this.availableLocales = Collections.singletonList(Locale.getDefault());
        } else {
            this.availableLocales = availableLocales.stream()
                    .map(LocaleResolver::resolve)
                    .collect(Collectors.toList());
        }
    }
}
