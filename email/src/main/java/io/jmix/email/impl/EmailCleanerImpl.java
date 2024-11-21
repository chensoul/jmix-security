/*
 * Copyright 2021 Haulmont.
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

package io.jmix.email.impl;

import io.jmix.core.filestore.FileStorage;
import io.jmix.email.EmailCleaner;
import io.jmix.email.EmailerProperties;
import io.jmix.email.entity.SendingMessage;
import io.jmix.email.repository.SendingMessageRepository;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;


@Component("email_EmailCleaner")
public class EmailCleanerImpl implements EmailCleaner {
    private static final Logger log = LoggerFactory.getLogger(EmailCleanerImpl.class);

    @Autowired
    private EmailerProperties emailerProperties;

    @Autowired
    private SendingMessageRepository sendingMessageRepository;

    @Autowired
    private FileStorage fileStorage;

    @Transactional
    @Override
    public Integer deleteOldEmails() {
        log.trace("Start deletion of old emails...");
        int maxAgeOfImportantMessages = emailerProperties.getMaxAgeOfImportantMessages();
        int maxAgeOfNonImportantMessages = emailerProperties.getMaxAgeOfNonImportantMessages();

        int result = 0;
        if (maxAgeOfNonImportantMessages!=0) {
            result += deleteMessages(maxAgeOfNonImportantMessages, false);
        }

        if (maxAgeOfImportantMessages!=0) {
            result += deleteMessages(maxAgeOfImportantMessages, true);
        }

        log.trace("{} emails was deleted", result);
        return result;
    }

    private int deleteMessages(int ageOfMessage, boolean important) {

        List<SendingMessage> messagesToDelete = sendingMessageRepository.findByImportantAndCreateTsLessThan(
                important, Date.from(ZonedDateTime.now().minusDays(ageOfMessage).toInstant()));

        if (emailerProperties.isCleanFileStorage()) {
            messagesToDelete.forEach(msg -> {
                msg.getAttachments().stream()
                        .filter(attachment -> attachment.getContentFile()!=null)
                        .forEach(attachment -> fileStorage.removeFile(attachment.getContentFile()));
                if (msg.getContentTextFile()!=null) {
                    fileStorage.removeFile(msg.getContentTextFile());
                }
            });
        }

        if (!messagesToDelete.isEmpty()) {
            List<SendingMessage> sendingMessages = sendingMessageRepository.findAllById(messagesToDelete.stream().map(SendingMessage::getId).collect(Collectors.toList()));
            sendingMessageRepository.deleteAllInBatch(sendingMessages);
            return sendingMessages.size();
        } else {
            return 0;
        }
    }
}
