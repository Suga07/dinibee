package com.bookeat.auth.entity;
import com.bookeat.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="refresh_tokens") @Getter @Setter @NoArgsConstructor
public class RefreshToken extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="session_id", nullable=false) private AuthSession session;
    @Column(nullable=false, unique=true, length=64) private String tokenHash;
    @Column(nullable=false) private Instant expiresAt;
    private Instant consumedAt;
}
