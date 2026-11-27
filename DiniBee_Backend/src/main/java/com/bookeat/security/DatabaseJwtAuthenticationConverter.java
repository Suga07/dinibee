package com.bookeat.security;
import com.bookeat.auth.repository.AuthSessionRepository;
import com.bookeat.user.entity.UserStatus;
import com.bookeat.user.mapper.UserMapper;
import com.bookeat.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@Component @RequiredArgsConstructor
public class DatabaseJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final UserMapper mapper;
    private final Clock clock;
    @Override @Transactional(readOnly=true)
    public AbstractAuthenticationToken convert(Jwt jwt) {
        UUID userId; UUID sessionId;
        try {
            userId = UUID.fromString(jwt.getSubject());
            sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
        } catch (IllegalArgumentException | NullPointerException ex) { throw invalid(); }
        var user = users.findById(userId).orElseThrow(this::invalid);
        var session = sessions.findById(sessionId).orElseThrow(this::invalid);
        Object version = jwt.getClaim("ver");
        if (!(version instanceof Number number) || number.longValue() != user.getTokenVersion()
                || user.getStatus() != UserStatus.ACTIVE || session.getRevokedAt() != null
                || !session.getExpiresAt().isAfter(clock.instant()) || !session.getUser().getId().equals(userId)) throw invalid();
        var authorities = new HashSet<SimpleGrantedAuthority>();
        mapper.roles(user).forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
        mapper.permissions(user).forEach(p -> authorities.add(new SimpleGrantedAuthority(p)));
        // Use current database grants so role changes do not wait for JWT expiry.
        return UsernamePasswordAuthenticationToken.authenticated(new AppPrincipal(userId, sessionId), null, authorities);
    }
    private OAuth2AuthenticationException invalid() {
        return new OAuth2AuthenticationException(new OAuth2Error("invalid_token"), "Session is invalid or expired");
    }
}
