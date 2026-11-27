package com.bookeat.auth.response;
import com.bookeat.user.response.UserResponse;
import java.time.Instant;

public record AuthResponse(String tokenType, String accessToken, Instant accessTokenExpiresAt,
        String refreshToken, Instant refreshTokenExpiresAt, UserResponse user) {
    @Override public String toString() { return "AuthResponse[redacted]"; }
}
