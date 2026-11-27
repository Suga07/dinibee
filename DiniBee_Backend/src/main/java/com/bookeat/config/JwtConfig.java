package com.bookeat.config;
import com.bookeat.common.security.SecretTokens;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.context.annotation.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import javax.crypto.spec.SecretKeySpec;
import java.time.*;

@Configuration
public class JwtConfig {
    @Bean public JwtEncoder jwtEncoder(AuthProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(SecretTokens.decodeKey(properties.jwtSecret())));
    }
    @Bean public JwtDecoder jwtDecoder(AuthProperties properties, Clock clock) {
        var key = new SecretKeySpec(SecretTokens.decodeKey(properties.jwtSecret()), "HmacSHA256");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        JwtTimestampValidator timestamp = new JwtTimestampValidator(Duration.ofSeconds(30));
        timestamp.setClock(clock);
        OAuth2TokenValidator<Jwt> required = jwt -> {
            boolean valid = jwt.getExpiresAt() != null && jwt.getIssuedAt() != null && jwt.getSubject() != null
                    && jwt.getAudience() != null && jwt.getAudience().contains(properties.audience())
                    && "access".equals(jwt.getClaimAsString("token_use"))
                    && !jwt.getIssuedAt().isAfter(clock.instant().plusSeconds(30));
            return valid ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Required claims are invalid", null));
        };
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(timestamp, new JwtIssuerValidator(properties.issuer()), required));
        return decoder;
    }
}
