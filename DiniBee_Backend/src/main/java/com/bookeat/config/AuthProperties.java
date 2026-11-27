package com.bookeat.config;
import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;

@Validated @ConfigurationProperties("app.auth")
public record AuthProperties(@NotBlank String issuer, @NotBlank String audience,
        @NotBlank String jwtSecret, @NotBlank String tokenPepper,
        @NotNull Duration accessTtl, @NotNull Duration refreshTtl,
        @NotNull Duration otpTtl, @NotNull Duration resetTtl,
        @Min(1) @Max(10) int maxChallengeAttempts) {
    public AuthProperties {
        if (accessTtl != null && (accessTtl.isNegative() || accessTtl.isZero() || accessTtl.compareTo(Duration.ofMinutes(30)) > 0))
            throw new IllegalArgumentException("Access TTL must be positive and at most 30 minutes");
        for (Duration duration : new Duration[]{refreshTtl, otpTtl, resetTtl}) {
            if (duration != null && (duration.isZero() || duration.isNegative()))
                throw new IllegalArgumentException("Token TTLs must be positive");
        }
    }
    @Override public String toString() { return "AuthProperties[redacted]"; }
}
