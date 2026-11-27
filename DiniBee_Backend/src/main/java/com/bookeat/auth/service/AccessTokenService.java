package com.bookeat.auth.service;
import com.bookeat.auth.entity.AuthSession;
import com.bookeat.config.AuthProperties;
import com.bookeat.user.entity.User;
import com.bookeat.user.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor
public class AccessTokenService {
    private final JwtEncoder encoder;
    private final AuthProperties properties;
    private final UserMapper mapper;
    private final Clock clock;
    public IssuedToken issue(User user, AuthSession session) {
        Instant now = clock.instant();
        Instant expiry = now.plus(properties.accessTtl());
        if (expiry.isAfter(session.getExpiresAt())) expiry = session.getExpiresAt();
        JwtClaimsSet claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(user.getId().toString()).issuedAt(now).notBefore(now).expiresAt(expiry).id(UUID.randomUUID().toString())
                .claim("userId", user.getId().toString()).claim("email", user.getEmail()).claim("roles", mapper.roles(user))
                .claim("permissions", mapper.permissions(user)).claim("ver", user.getTokenVersion())
                .claim("sid", session.getId().toString()).claim("token_use", "access").build();
        String value = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
        return new IssuedToken(value, expiry);
    }
    public record IssuedToken(String value, Instant expiresAt) {
        @Override public String toString() { return "IssuedToken[redacted]"; }
    }
}
