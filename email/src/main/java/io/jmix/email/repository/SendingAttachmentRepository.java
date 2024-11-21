package io.jmix.email.repository;

import io.jmix.email.entity.SendingAttachment;
import io.jmix.email.entity.SendingMessage;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * TODO Comment
 *
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since TODO
 */
@Repository
public interface SendingAttachmentRepository extends JpaRepository<SendingAttachment, UUID> {
}
