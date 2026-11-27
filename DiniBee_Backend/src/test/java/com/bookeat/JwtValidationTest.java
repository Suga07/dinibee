package com.bookeat;
import com.bookeat.config.*;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class JwtValidationTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-11T10:00:00Z"),ZoneOffset.UTC);
    private final String key=Base64.getEncoder().encodeToString(new byte[32]);
    private final AuthProperties properties=new AuthProperties("bookeat-api","bookeat-clients",key,key,
            Duration.ofMinutes(15),Duration.ofDays(30),Duration.ofMinutes(10),Duration.ofMinutes(15),5);
    private final JwtConfig config=new JwtConfig();
    private String token(String issuer, String audience, String use, Instant expiry, boolean includeExpiry) {
        var claims=JwtClaimsSet.builder().subject(UUID.randomUUID().toString()).issuer(issuer)
                .audience(List.of(audience)).issuedAt(clock.instant().minusSeconds(600)).claim("token_use",use);
        if(includeExpiry) claims.expiresAt(expiry);
        return config.jwtEncoder(properties).encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims.build())).getTokenValue();
    }
    @Test void validAccessTokenIsAccepted() {
        assertThat(config.jwtDecoder(properties,clock).decode(token("bookeat-api","bookeat-clients","access",clock.instant().plusSeconds(300),true)).getSubject()).isNotBlank();
    }
    @Test void expiredTokenIsRejected() {
        assertThatThrownBy(() -> config.jwtDecoder(properties,clock).decode(token("bookeat-api","bookeat-clients","access",clock.instant().minusSeconds(120),true))).isInstanceOf(JwtException.class);
    }
    @Test void missingExpiryIsRejected() {
        assertThatThrownBy(() -> config.jwtDecoder(properties,clock).decode(token("bookeat-api","bookeat-clients","access",null,false))).isInstanceOf(JwtException.class);
    }
    @Test void wrongIssuerAndAudienceAndTokenPurposeAreRejected() {
        for(var values:List.of(List.of("wrong","bookeat-clients","access"),List.of("bookeat-api","wrong","access"),List.of("bookeat-api","bookeat-clients","refresh")))
            assertThatThrownBy(() -> config.jwtDecoder(properties,clock).decode(token(values.get(0),values.get(1),values.get(2),clock.instant().plusSeconds(300),true))).isInstanceOf(JwtException.class);
    }
    @Test void differentSigningKeyIsRejected() {
        byte[] alternate=new byte[32]; Arrays.fill(alternate,(byte)1);
        var other=new AuthProperties("bookeat-api","bookeat-clients",Base64.getEncoder().encodeToString(alternate),key,
                Duration.ofMinutes(15),Duration.ofDays(30),Duration.ofMinutes(10),Duration.ofMinutes(15),5);
        assertThatThrownBy(() -> config.jwtDecoder(other,clock).decode(token("bookeat-api","bookeat-clients","access",clock.instant().plusSeconds(300),true))).isInstanceOf(JwtException.class);
    }
}
