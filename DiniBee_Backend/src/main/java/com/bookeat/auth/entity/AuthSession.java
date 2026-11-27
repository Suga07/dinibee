package com.bookeat.auth.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="auth_sessions") @Getter @Setter @NoArgsConstructor
public class AuthSession extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Column(nullable=false) private Instant expiresAt;
    private Instant revokedAt;
    @Column(nullable=false) private Instant lastUsedAt;
}
