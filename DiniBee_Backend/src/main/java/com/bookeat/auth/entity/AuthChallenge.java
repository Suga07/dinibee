package com.bookeat.auth.entity;
import com.bookeat.common.entity.BaseEntity;
import com.bookeat.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity @Table(name="auth_challenges") @Getter @Setter @NoArgsConstructor
public class AuthChallenge extends BaseEntity {
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="user_id", nullable=false) private User user;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=30) private ChallengePurpose purpose;
    @Column(nullable=false, length=64) private String secretHash;
    @Column(nullable=false) private Instant expiresAt;
    @Column(nullable=false) private int attempts;
    private Instant consumedAt;
}
