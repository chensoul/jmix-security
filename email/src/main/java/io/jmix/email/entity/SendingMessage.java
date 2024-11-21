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

package io.jmix.email.entity;

import io.jmix.core.filestore.FileRef;
import io.jmix.email.SendingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

/**
 * Entity to store information about sending emails.
 */
@Data
@Entity(name = "email_SendingMessage")
@Table(name = "EMAIL_SENDING_MESSAGE")
public class SendingMessage implements Serializable {

    private static final long serialVersionUID = -8156998515878702538L;

    public static final int SUBJECT_LENGTH = 500;
    public static final int BODY_CONTENT_TYPE_LENGTH = 50;
    public static final String HEADERS_SEPARATOR = "\n";

    @Id
    @Column(name = "ID", nullable = false)
    private UUID id;

    @Version
    @Column(name = "VERSION", nullable = false)
    private Integer version;

    @CreatedDate
    @Column(name = "CREATE_TS")
    private Date createTs;

    @CreatedBy
    @Column(name = "CREATED_BY", length = 50)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "UPDATE_TS")
    private Date updateTs;

    @LastModifiedBy
    @Column(name = "UPDATED_BY", length = 50)
    private String updatedBy;

    @Column(name = "DELETE_TS")
    private Date deleteTs;

    @Column(name = "DELETED_BY", length = 50)
    private String deletedBy;

    @Column(name = "ADDRESS_TO")
    protected String address;

    @Column(name = "ADDRESS_FROM")
    protected String from;

    @Column(name = "ADDRESS_CC")
    protected String cc;

    @Column(name = "ADDRESS_BCC")
    protected String bcc;

    @Column(name = "SUBJECT", length = SUBJECT_LENGTH)
    protected String subject;

    /**
     * Email body is stored either in this field or in {@link #contentTextFile}.
     */
    @Column(name = "CONTENT_TEXT")
    protected String contentText;

    @Column(name = "CONTENT_TEXT_FILE")
    protected FileRef contentTextFile;

    @Column(name = "STATUS")
    protected Integer status;

    @Column(name = "DATE_SENT")
    protected Date dateSent;

    @Column(name = "ATTACHMENTS_NAME")
    protected String attachmentsName;

    @Column(name = "DEADLINE")
    protected Date deadline;

    @Column(name = "ATTEMPTS_LIMIT")
    protected Integer attemptsLimit;

    @Column(name = "ATTEMPTS_MADE")
    protected Integer attemptsMade;

    @OneToMany(mappedBy = "message")
    protected List<SendingAttachment> attachments;

    @Column(name = "EMAIL_HEADERS")
    protected String headers;

    @Column(name = "BODY_CONTENT_TYPE", length = BODY_CONTENT_TYPE_LENGTH)
    protected String bodyContentType;

    @Column(name = "SYS_TENANT_ID")
    protected String sysTenantId;

    @Column(name = "IMPORTANT")
    protected Boolean important = false;

    @PrePersist
    protected void initLastAttemptTime() {
        if (getStatus()!=null && getStatus()==SendingStatus.QUEUE.getId() && getAttemptsMade()==0) {
            setUpdateTs(null);
            setUpdatedBy(null);
        }
    }

}
