package com.bookeat.auth.repository;
import com.bookeat.auth.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.*;

public interface AuthChallengeRepository extends JpaRepository<AuthChallenge, UUID> {
    @Query("select c.user.id from AuthChallenge c where c.id = :id")
    Optional<UUID> findUserIdById(@Param("id") UUID id);
    Optional<AuthChallenge> findFirstByUserIdAndPurposeOrderByCreatedAtDesc(UUID userId, ChallengePurpose purpose);
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE auth_challenges SET consumed_at = :now, updated_at = :now, version = version + 1 WHERE user_id = :userId AND purpose = :#{#purpose.name()} AND consumed_at IS NULL", nativeQuery = true)
    int invalidate(@Param("userId") UUID userId, @Param("purpose") ChallengePurpose purpose, @Param("now") Instant now);
}
