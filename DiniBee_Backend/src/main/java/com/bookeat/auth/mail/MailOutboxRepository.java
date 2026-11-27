package com.bookeat.auth.mail;
import org.springframework.data.jpa.repository.*;
import java.util.*;
public interface MailOutboxRepository extends JpaRepository<MailOutboxEntry, UUID> {
    @Query(value="select * from mail_outbox where status = 'PENDING' and next_attempt_at <= CURRENT_TIMESTAMP order by created_at limit 10 for update skip locked", nativeQuery=true)
    List<MailOutboxEntry> lockNextBatch();
}
