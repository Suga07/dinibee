package com.bookeat.auth.repository;
import com.bookeat.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
import java.time.Instant;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {
    @Modifying
    @Query("update AuthSession s set s.revokedAt = :now, s.updatedAt = :now, s.version = s.version + 1 where s.user.id = :userId and s.revokedAt is null")
    int revokeAll(@Param("userId") UUID userId, @Param("now") Instant now);
}
