package com.bookeat.auth.repository;
import com.bookeat.auth.entity.RefreshToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    @Query("select t.session.user.id from RefreshToken t where t.tokenHash = :hash")
    Optional<UUID> findUserIdByTokenHash(@Param("hash") String hash);
}
