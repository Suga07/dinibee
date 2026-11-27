package com.bookeat.security;
import com.bookeat.common.security.SecretTokens;
import com.bookeat.config.AuthProperties;
import com.bookeat.exception.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Duration;

@Service
public class RateLimitService {
    private final JdbcTemplate jdbc;
    private final byte[] key;
    public RateLimitService(JdbcTemplate jdbc, AuthProperties properties) {
        this.jdbc = jdbc;
        this.key = SecretTokens.decodeKey(properties.tokenPepper());
    }
    // Failed authentication must not roll back its rate-limit counter.
    @Transactional(propagation=Propagation.REQUIRES_NEW, noRollbackFor=ApiException.class)
    public void consume(String scope, String identity, int limit, Duration window) {
        String bucket = SecretTokens.hmac(key, "rate:" + scope + ":" + identity);
        Integer hits = jdbc.queryForObject("""
            INSERT INTO auth_rate_limits(bucket_key, window_started_at, hits)
            VALUES (?, CURRENT_TIMESTAMP, 1)
            ON CONFLICT (bucket_key) DO UPDATE SET
                hits = CASE WHEN auth_rate_limits.window_started_at <= CURRENT_TIMESTAMP - (? * interval '1 second')
                    THEN 1 ELSE LEAST(auth_rate_limits.hits + 1, 1000000) END,
                window_started_at = CASE WHEN auth_rate_limits.window_started_at <= CURRENT_TIMESTAMP - (? * interval '1 second')
                    THEN CURRENT_TIMESTAMP ELSE auth_rate_limits.window_started_at END
            RETURNING hits
            """, Integer.class, bucket, window.toSeconds(), window.toSeconds());
        if (hits == null || hits > limit) throw new ApiException(ErrorCode.RATE_LIMITED);
    }
    @Scheduled(fixedDelay=3600000)
    public void cleanup() {
        jdbc.update("DELETE FROM auth_rate_limits WHERE window_started_at < CURRENT_TIMESTAMP - interval '1 day'");
    }
}
