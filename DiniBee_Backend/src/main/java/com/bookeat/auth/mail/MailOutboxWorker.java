package com.bookeat.auth.mail;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.*;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;

@Slf4j @Component @ConditionalOnProperty(name="app.mail.enabled", havingValue="true", matchIfMissing=true)
public class MailOutboxWorker {
    private final MailOutboxRepository repository;
    private final MailOutboxService outbox;
    private final JavaMailSender sender;
    private final Clock clock;
    private final String from;
    public MailOutboxWorker(MailOutboxRepository repository, MailOutboxService outbox, JavaMailSender sender,
            Clock clock, @Value("${app.mail.from}") String from) {
        this.repository=repository; this.outbox=outbox; this.sender=sender; this.clock=clock; this.from=from;
    }
    @Scheduled(fixedDelayString="${app.mail.poll-delay-ms:5000}") @Transactional
    public void deliver() {
        for (var entry : repository.lockNextBatch()) {
            if (!entry.getExpiresAt().isAfter(clock.instant())) {
                entry.setStatus(MailOutboxEntry.Status.EXPIRED); entry.setEncryptedBody(null); continue;
            }
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from); message.setTo(entry.getRecipient()); message.setSubject(entry.getSubject());
                message.setText(outbox.decrypt(entry)); sender.send(message);
                entry.setStatus(MailOutboxEntry.Status.SENT); entry.setSentAt(clock.instant()); entry.setEncryptedBody(null);
            } catch (MailException | IllegalArgumentException ex) {
                entry.setAttempts(entry.getAttempts() + 1);
                if (entry.getAttempts() >= 5) {
                    entry.setStatus(MailOutboxEntry.Status.FAILED); entry.setEncryptedBody(null);
                    log.error("Verification mail failed permanently entryId={}", entry.getId());
                } else {
                    entry.setNextAttemptAt(clock.instant().plusSeconds(15L * (1L << entry.getAttempts())));
                    log.warn("Verification mail retry scheduled entryId={} attempt={}", entry.getId(), entry.getAttempts());
                }
            }
        }
    }
}
