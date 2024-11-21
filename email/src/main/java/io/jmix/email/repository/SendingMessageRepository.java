package io.jmix.email.repository;

import io.jmix.email.entity.SendingMessage;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * TODO Comment
 *
 * @author <a href="mailto:ichensoul@gmail.com">chensoul</a>
 * @since TODO
 */
@Repository
public interface SendingMessageRepository extends JpaRepository<SendingMessage, UUID> {
    List<SendingMessage> findByImportantAndCreateTsLessThan(boolean important, Date date);


    @Query("select sm from email_SendingMessage sm" +
            " where sm.status = :statusQueue or (sm.status = :statusSending and sm.updateTs < :time)" +
            " order by sm.createTs")
    List<SendingMessage> findByStatusAndTime(Integer statusQueue, Integer statusSending, Date time);

}
