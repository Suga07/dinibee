package com.bookeat.auth.mail;
import com.bookeat.common.security.EncryptedText;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;

@Service
public class MailOutboxService {
    private final MailOutboxRepository repository;
    private final EncryptedText encryption;
    private final Clock clock;
    public MailOutboxService(MailOutboxRepository repository, Clock clock, @Value("${app.mail.encryption-key}") String key) {
        this.repository = repository; this.clock = clock; this.encryption = new EncryptedText(key);
    }
    @Transactional(propagation=Propagation.MANDATORY)
    public void enqueue(String recipient, String subject, String body, Instant expiry) {
        MailOutboxEntry entry = new MailOutboxEntry();
        entry.setRecipient(recipient); entry.setSubject(subject); entry.setEncryptedBody(encryption.encrypt(body));
        entry.setNextAttemptAt(clock.instant()); entry.setExpiresAt(expiry); repository.save(entry);
    }
    public String decrypt(MailOutboxEntry entry) { return encryption.decrypt(entry.getEncryptedBody()); }
}
