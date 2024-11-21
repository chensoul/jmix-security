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


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.io.Serializable;
import java.util.Date;
import java.util.UUID;
import lombok.Data;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@Entity(name = "email_SendingAttachment")
@Table(name = "EMAIL_SENDING_ATTACHMENT")
public class SendingAttachment implements Serializable {
    private static final long serialVersionUID = -8253918579521701435L;

    @Id
    @Column(name = "ID")
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "MESSAGE_ID")
    protected SendingMessage message;

    /**
     * Attachment data is stored either in this field or in {@link #contentFile}
     */
    @Column(name = "CONTENT")
    protected byte[] content;

    @Column(name = "CONTENT_FILE")
    protected String contentFile;

    @Column(name = "NAME", length = 500)
    protected String name;

    @Column(name = "CONTENT_ID", length = 50)
    protected String contentId;

    @Column(name = "DISPOSITION", length = 50)
    protected String disposition;

    @Column(name = "TEXT_ENCODING", length = 50)
    protected String encoding;

    @Column(name = "SYS_TENANT_ID")
    protected String sysTenantId;

}