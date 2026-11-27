package com.bookeat.auth.mail;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="mail_outbox") @Getter @Setter @NoArgsConstructor
public class MailOutboxEntry extends BaseEntity {
    @Column(nullable=false, length=320) private String recipient;
    @Column(nullable=false, length=160) private String subject;
    @Column(columnDefinition="text") private String encryptedBody;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status = Status.PENDING;
    @Column(nullable=false) private int attempts;
    @Column(nullable=false) private Instant nextAttemptAt;
    @Column(nullable=false) private Instant expiresAt;
    private Instant sentAt;
    public enum Status { PENDING, SENT, FAILED, EXPIRED }
}
