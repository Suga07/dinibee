package com.bookeat.auth.service.impl;

import com.bookeat.auth.entity.*;
import com.bookeat.auth.mail.MailOutboxService;
import com.bookeat.auth.repository.*;
import com.bookeat.auth.request.*;
import com.bookeat.auth.response.*;
import com.bookeat.auth.service.*;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.security.SecretTokens;
import com.bookeat.common.validation.PasswordRules;
import com.bookeat.config.AuthProperties;
import com.bookeat.customer.entity.Customer;
import com.bookeat.customer.repository.CustomerRepository;
import com.bookeat.exception.*;
import com.bookeat.owner.entity.RestaurantOwner;
import com.bookeat.owner.repository.RestaurantOwnerRepository;
import com.bookeat.security.*;
import com.bookeat.user.entity.*;
import com.bookeat.user.mapper.UserMapper;
import com.bookeat.user.repository.*;
import com.bookeat.user.response.UserResponse;
import com.bookeat.restaurant.repository.RestaurantRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
@Transactional(noRollbackFor={ChallengeRejectedException.class, TokenReplayException.class})
public class AuthServiceImpl implements AuthService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final CustomerRepository customers;
    private final RestaurantOwnerRepository owners;
    private final AuthSessionRepository sessions;
    private final RefreshTokenRepository refreshTokens;
    private final AuthChallengeRepository challenges;
    private final PasswordEncoder passwords;
    private final AccessTokenService accessTokens;
    private final UserMapper mapper;
    private final AuthProperties properties;
    private final MailOutboxService mail;
    private final MyDreamsSmsService sms;
    private final RateLimitService limiter;
    private final AuditService audit;
    private final RestaurantRepository restaurantRepository;
    private final Clock clock;
    private String dummyPasswordHash;
    private byte[] pepper;

    @PostConstruct
    void initialize() {
        dummyPasswordHash = passwords.encode(SecretTokens.randomToken());
        pepper = SecretTokens.decodeKey(properties.tokenPepper());
    }
    @Override
    public AuthResponse register(RegisterRequest request, RoleCode role, String ip) {
        if (role != RoleCode.CUSTOMER && role != RoleCode.OWNER) throw new ApiException(ErrorCode.ACCESS_DENIED);
        String email = normalizeEmail(request.email());
        if (users.existsByEmail(email)) throw new ApiException(ErrorCode.USER_ALREADY_EXISTS);
        User user = new User();
        user.setFirstName(request.firstName().trim()); user.setLastName(request.lastName().trim());
        user.setEmail(email); user.setPhoneNumber(request.phoneNumber()); user.setPasswordHash(passwords.encode(request.password()));
        user.getRoles().add(roles.findByCode(role).orElseThrow(() -> new IllegalStateException("Required role migration missing")));
        users.saveAndFlush(user);
        if (role == RoleCode.CUSTOMER) { Customer customer = new Customer(); customer.setUser(user); customers.save(customer); }
        else { RestaurantOwner owner = new RestaurantOwner(); owner.setUser(user); owners.save(owner); }
        audit.record(user.getId(), "USER_REGISTERED", "USER", user.getId(), ip);
        return newSession(user);
    }
    @Override
    public AuthResponse login(LoginRequest request, String ip) {
        String identifier = request.email().trim();
        limiter.consume("login-account", identifier.toLowerCase(Locale.ROOT), 10, Duration.ofMinutes(15));
        User user = null;
        if (identifier.contains("@")) {
            user = users.lockByEmail(normalizeEmail(identifier)).orElse(null);
        } else {
            String phone = normalizePhone(identifier);
            var found = users.lockByPhoneNumberIn(List.of(phone, "+91" + phone, "91" + phone));
            if (!found.isEmpty()) {
                user = found.get(0);
            } else {
                var venues = restaurantRepository.findByPhoneOrAlternatePhoneIn(List.of(phone, "+91" + phone, "91" + phone, "+" + phone));
                for (var v : venues) {
                    if (v.getOwner() != null && v.getOwner().getUser() != null) {
                        user = users.lockById(v.getOwner().getUser().getId()).orElse(v.getOwner().getUser());
                        break;
                    }
                }
            }
        }
        String expected = user == null ? dummyPasswordHash : user.getPasswordHash();
        boolean matches = passwords.matches(request.password(), expected);
        if (user == null || !matches || user.getStatus() != UserStatus.ACTIVE) {
            log.warn("Login rejected reference={}", UUID.randomUUID());
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }
        audit.record(user.getId(), "LOGIN_SUCCEEDED", "USER", user.getId(), ip);
        return newSession(user);
    }

    @Value("${app.oauth.google.client-id:}")
    private String googleClientId;

    @Value("${app.oauth.apple.client-id:}")
    private String appleClientId;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public AuthResponse googleLogin(GoogleLoginRequest request, String ip) {
        String email = null;
        String firstName = request.firstName();
        String lastName = request.lastName();

        if (request.idToken() != null && !request.idToken().isBlank()) {
            String token = request.idToken().trim();
            try {
                String url = token.contains(".")
                    ? "https://oauth2.googleapis.com/tokeninfo?id_token=" + token
                    : "https://oauth2.googleapis.com/tokeninfo?access_token=" + token;
                Map<?, ?> payload = restTemplate.getForObject(url, Map.class);
                if (payload != null) {
                    if (payload.get("email") != null) {
                        email = payload.get("email").toString();
                    }
                    if (payload.get("given_name") != null) {
                        firstName = payload.get("given_name").toString();
                    }
                    if (payload.get("family_name") != null) {
                        lastName = payload.get("family_name").toString();
                    } else if (payload.get("name") != null && (lastName == null || lastName.isBlank())) {
                        lastName = payload.get("name").toString();
                    }
                }
            } catch (Exception e) {
                log.warn("Google token verification failed: {}", e.getMessage());
            }

            // Resilient JWT payload decoding fallback if tokeninfo call times out or fails
            if ((email == null || email.isBlank()) && request.idToken().contains(".")) {
                try {
                    String[] parts = request.idToken().trim().split("\\.");
                    if (parts.length >= 2) {
                        byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                        Map<?, ?> jwtPayload = new com.fasterxml.jackson.databind.ObjectMapper().readValue(decoded, Map.class);
                        if (jwtPayload.get("email") != null) {
                            email = jwtPayload.get("email").toString();
                        }
                        if (firstName == null || firstName.isBlank()) {
                            if (jwtPayload.get("given_name") != null) firstName = jwtPayload.get("given_name").toString();
                        }
                        if (lastName == null || lastName.isBlank()) {
                            if (jwtPayload.get("family_name") != null) lastName = jwtPayload.get("family_name").toString();
                            else if (jwtPayload.get("name") != null) lastName = jwtPayload.get("name").toString();
                        }
                    }
                } catch (Exception ex) {
                    log.warn("JWT payload decoding fallback failed: {}", ex.getMessage());
                }
            }
        }

        if ((email == null || email.isBlank()) && request.email() != null && !request.email().isBlank()) {
            email = request.email();
        }

        if (email == null || email.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        String normalizedEmail = normalizeEmail(email);
        User user = users.lockByEmail(normalizedEmail).orElse(null);
        boolean allowCreate = !Boolean.FALSE.equals(request.createIfMissing());

        if (user == null) {
            if (!allowCreate) {
                log.info("Google login rejected: no account found for email={}", normalizedEmail);
                throw new ApiException(ErrorCode.AUTH_ACCOUNT_NOT_FOUND);
            }
            try {
                user = new User();
                user.setFirstName(firstName != null && !firstName.isBlank() ? firstName.trim() : "Google");
                user.setLastName(lastName != null && !lastName.isBlank() ? lastName.trim() : "User");
                user.setEmail(normalizedEmail);
                user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
                user.setEmailVerified(true);
                user.setStatus(UserStatus.ACTIVE);
                user.getRoles().add(roles.findByCode(RoleCode.CUSTOMER).orElseThrow(() -> new IllegalStateException("Required role migration missing")));
                users.saveAndFlush(user);

                Customer customer = new Customer();
                customer.setUser(user);
                customers.save(customer);

                audit.record(user.getId(), "USER_REGISTERED_GOOGLE", "USER", user.getId(), ip);
            } catch (org.springframework.dao.DataIntegrityViolationException dive) {
                // Concurrent registration handled gracefully
                user = users.findByEmail(normalizedEmail).orElseThrow(() -> dive);
            }
        } else {
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Inactive user attempted Google login: {}", user.getId());
                throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                users.save(user);
            }
            audit.record(user.getId(), "LOGIN_SUCCEEDED_GOOGLE", "USER", user.getId(), ip);
        }

        // Guarantee customer record exists
        if (customers.findByUserId(user.getId()).isEmpty()) {
            Customer customer = new Customer();
            customer.setUser(user);
            customers.save(customer);
        }

        return newSession(user);
    }

    @Override
    public AuthResponse appleLogin(AppleLoginRequest request, String ip) {
        String email = null;
        String appleSub = request.userIdentifier();
        String firstName = request.firstName();
        String lastName = request.lastName();

        if (request.identityToken() != null && !request.identityToken().isBlank()) {
            try {
                String[] parts = request.identityToken().trim().split("\\.");
                if (parts.length >= 2) {
                    byte[] decoded = Base64.getUrlDecoder().decode(parts[1]);
                    String json = new String(decoded, StandardCharsets.UTF_8);
                    Map<?, ?> payload = objectMapper.readValue(json, Map.class);
                    if (payload != null) {
                        if (payload.get("email") != null) {
                            email = payload.get("email").toString();
                        }
                        if (payload.get("sub") != null && (appleSub == null || appleSub.isBlank())) {
                            appleSub = payload.get("sub").toString();
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("Apple token payload decode failed: {}", e.getMessage());
            }
        }

        if ((email == null || email.isBlank()) && request.email() != null && !request.email().isBlank()) {
            email = request.email();
        }

        if ((email == null || email.isBlank()) && appleSub != null && !appleSub.isBlank()) {
            email = appleSub.toLowerCase(Locale.ROOT) + "@apple.dinibee.tech";
        }

        if (email == null || email.isBlank()) {
            throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        }

        String normalizedEmail = normalizeEmail(email);
        User user = users.lockByEmail(normalizedEmail).orElse(null);

        if (user == null) {
            user = new User();
            user.setFirstName(firstName != null && !firstName.isBlank() ? firstName.trim() : "Apple");
            user.setLastName(lastName != null && !lastName.isBlank() ? lastName.trim() : "User");
            user.setEmail(normalizedEmail);
            user.setPasswordHash(passwords.encode(UUID.randomUUID().toString()));
            user.setEmailVerified(true);
            user.setStatus(UserStatus.ACTIVE);
            user.getRoles().add(roles.findByCode(RoleCode.CUSTOMER).orElseThrow(() -> new IllegalStateException("Required role migration missing")));
            users.saveAndFlush(user);

            Customer customer = new Customer();
            customer.setUser(user);
            customers.save(customer);

            audit.record(user.getId(), "USER_REGISTERED_APPLE", "USER", user.getId(), ip);
        } else {
            if (user.getStatus() != UserStatus.ACTIVE) {
                log.warn("Inactive user attempted Apple login: {}", user.getId());
                throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
            }
            if (!user.isEmailVerified()) {
                user.setEmailVerified(true);
                users.save(user);
            }
            audit.record(user.getId(), "LOGIN_SUCCEEDED_APPLE", "USER", user.getId(), ip);
        }

        return newSession(user);
    }
    @Override
    public AuthResponse refresh(RefreshRequest request, String ip) {
        String hash = SecretTokens.sha256(request.refreshToken());
        // Projection first: do not cache the token or session before acquiring the user lock.
        UUID userId = refreshTokens.findUserIdByTokenHash(hash).orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID));
        User user = lockActiveUser(userId);
        RefreshToken token = refreshTokens.findByTokenHash(hash).orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID));
        AuthSession session = token.getSession();
        Instant now = clock.instant();
        if (session.getRevokedAt() != null || !session.getExpiresAt().isAfter(now)) throw new ApiException(ErrorCode.AUTH_SESSION_REVOKED);
        if (token.getConsumedAt() != null) {
            session.setRevokedAt(now);
            audit.record(userId, "REFRESH_TOKEN_REUSED", "AUTH_SESSION", session.getId(), ip);
            // Dedicated exception is excluded from rollback, preserving session revocation.
            throw new TokenReplayException();
        }
        if (!token.getExpiresAt().isAfter(now)) throw new ApiException(ErrorCode.AUTH_TOKEN_INVALID);
        token.setConsumedAt(now); session.setLastUsedAt(now);
        return issuePair(user, session);
    }
    @Override
    public void logout(AppPrincipal principal, String ip) {
        lockActiveUser(principal.userId());
        AuthSession session = sessions.findById(principal.sessionId()).orElseThrow(() -> new ApiException(ErrorCode.AUTH_SESSION_REVOKED));
        if (!session.getUser().getId().equals(principal.userId())) throw new ApiException(ErrorCode.ACCESS_DENIED);
        session.setRevokedAt(clock.instant());
        audit.record(principal.userId(), "LOGOUT", "AUTH_SESSION", session.getId(), ip);
    }
    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        limiter.consume("forgot-account", email, 3, Duration.ofMinutes(15));
        var user = users.lockByEmail(email).orElse(null);
        if (user == null || user.getStatus() != UserStatus.ACTIVE) return;
        var recent = challenges.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), ChallengePurpose.PASSWORD_RESET);
        if (recent.isPresent() && recent.get().getCreatedAt().plusSeconds(60).isAfter(clock.instant())) return;
        String token = SecretTokens.randomToken();
        AuthChallenge challenge = createChallenge(user, ChallengePurpose.PASSWORD_RESET, token, properties.resetTtl());
        mail.enqueue(user.getEmail(), "Reset your BookEat password",
                "A password reset was requested for your BookEat account.\n\nChallenge ID: " + challenge.getId()
                + "\nReset token: " + token + "\nExpires at: " + challenge.getExpiresAt()
                + "\n\nEnter these in the app password reset screen. If you did not request this, ignore this email.", challenge.getExpiresAt());
    }
    @Override
    public void resetPassword(ResetPasswordRequest request, String ip) {
        UUID userId = challenges.findUserIdById(request.challengeId()).orElseThrow(ChallengeRejectedException::new);
        User user = lockActiveUser(userId);
        consumeChallenge(user, request.challengeId(), ChallengePurpose.PASSWORD_RESET, request.token());
        user.setPasswordHash(passwords.encode(request.newPassword()));
        user.setEmailVerified(true);
        revokeAllSessions(user);
        challenges.invalidate(userId, ChallengePurpose.PASSWORD_RESET, clock.instant());
        audit.record(userId, "PASSWORD_RESET", "USER", userId, ip);
    }
    @Override
    public void changePassword(UUID userId, ChangePasswordRequest request, String ip) {
        limiter.consume("change-password", userId.toString(), 5, Duration.ofMinutes(15));
        User user = lockActiveUser(userId);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash())) throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        user.setPasswordHash(passwords.encode(request.newPassword())); revokeAllSessions(user);
        challenges.invalidate(userId, ChallengePurpose.PASSWORD_RESET, clock.instant());
        audit.record(userId, "PASSWORD_CHANGED", "USER", userId, ip);
    }
    
    @Override
    public ChallengeResponse sendEmailOtp(UUID userId) {
        User user = lockActiveUser(userId);
        if (user.isEmailVerified()) throw new ApiException(ErrorCode.AUTH_ALREADY_VERIFIED);
        var recent = challenges.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(userId, ChallengePurpose.EMAIL_VERIFICATION);
        if (recent.isPresent() && recent.get().getCreatedAt().plusSeconds(60).isAfter(clock.instant())) throw new ApiException(ErrorCode.RATE_LIMITED);
        String code = SecretTokens.otp();
        AuthChallenge challenge = createChallenge(user, ChallengePurpose.EMAIL_VERIFICATION, code, properties.otpTtl());
        mail.enqueue(user.getEmail(), "Verify your BookEat email",
                "Your email verification code is: " + code + "\nChallenge ID: " + challenge.getId()
                + "\nExpires at: " + challenge.getExpiresAt() + "\nDo not share this code.", challenge.getExpiresAt());
        return new ChallengeResponse(challenge.getId(), challenge.getExpiresAt());
    }

    @Override
    public void verifyEmailOtp(UUID userId, VerifyEmailRequest request, String ip) {
        User user = lockActiveUser(userId);
        consumeChallenge(user, request.challengeId(), ChallengePurpose.EMAIL_VERIFICATION, request.code());
        user.setEmailVerified(true);
        audit.record(userId, "EMAIL_VERIFIED", "USER", userId, ip);
    }

    @Override
    public ChallengeResponse sendMobileOtp(MobileOtpSendRequest request) {
        String phone = normalizePhone(request.phoneNumber());
        RoleCode role;
        try { role = RoleCode.valueOf(request.role().trim().toUpperCase(Locale.ROOT)); }
        catch (Exception e) { throw new ApiException(ErrorCode.VALIDATION_FAILED); }
        if (role != RoleCode.CUSTOMER && role != RoleCode.OWNER) throw new ApiException(ErrorCode.ACCESS_DENIED);
        var found = users.findByPhoneNumber(phone);
        if (found.isEmpty()) found = users.findByPhoneNumber("+91" + phone);
        if (found.isEmpty()) found = users.findByPhoneNumber("91" + phone);
        User user = found.stream().filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode()==role)).findFirst().orElseThrow(() -> new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS));
        if (user.getStatus()!=UserStatus.ACTIVE) throw new ApiException(ErrorCode.AUTH_INVALID_CREDENTIALS);
        limiter.consume("mobile-otp", phone, 5, Duration.ofMinutes(15));
        var recent=challenges.findFirstByUserIdAndPurposeOrderByCreatedAtDesc(user.getId(), ChallengePurpose.MOBILE_LOGIN);
        if(recent.isPresent() && recent.get().getCreatedAt().plusSeconds(60).isAfter(clock.instant())) throw new ApiException(ErrorCode.RATE_LIMITED);
        if(recent.isPresent() && recent.get().getConsumedAt() == null) {
            recent.get().setConsumedAt(clock.instant());
            challenges.saveAndFlush(recent.get());
        }
        String code=SecretTokens.otp4();
        AuthChallenge challenge=createChallenge(user, ChallengePurpose.MOBILE_LOGIN, code, properties.otpTtl());
        sms.sendOtp(phone, code);
        return new ChallengeResponse(challenge.getId(), challenge.getExpiresAt());
    }
    @Override
    public AuthResponse verifyMobileOtp(MobileOtpVerifyRequest request, String ip) {
        UUID userId=challenges.findUserIdById(request.challengeId()).orElseThrow(ChallengeRejectedException::new);
        User user=lockActiveUser(userId);
        consumeChallenge(user, request.challengeId(), ChallengePurpose.MOBILE_LOGIN, request.code());
        audit.record(userId,"MOBILE_OTP_LOGIN_SUCCEEDED","USER",userId,ip);
        return newSession(user);
    }

    @Override @Transactional
    public UserResponse me(UUID userId) {
        User user = users.findById(userId).orElseThrow(() -> new ApiException(ErrorCode.USER_NOT_FOUND));
        if (user.getPhoneNumber() == null || user.getPhoneNumber().isBlank() || user.getPhoneNumber().contains("1234567890")) {
            var venues = restaurantRepository.findByOwnerUserId(userId, org.springframework.data.domain.Pageable.unpaged());
            if (!venues.isEmpty() && venues.getContent().get(0).getPhone() != null && !venues.getContent().get(0).getPhone().isBlank()) {
                user.setPhoneNumber(venues.getContent().get(0).getPhone().trim());
                users.saveAndFlush(user);
            }
        }
        return mapper.toResponse(user);
    }
    @Override
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request, String ip) {
        User user = lockActiveUser(userId);
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName().trim());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName().trim());
        }
        if (request.email() != null && !request.email().isBlank()) {
            String newEmail = normalizeEmail(request.email());
            if (!newEmail.equals(user.getEmail()) && users.existsByEmail(newEmail)) {
                throw new ApiException(ErrorCode.USER_ALREADY_EXISTS);
            }
            user.setEmail(newEmail);
        }
        if (request.password() != null && !request.password().isBlank()) {
            if (!PasswordRules.valid(request.password())) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED);
            }
            user.setPasswordHash(passwords.encode(request.password()));
            revokeAllSessions(user);
        }
        users.saveAndFlush(user);
        audit.record(userId, "PROFILE_UPDATED", "USER", userId, ip);
        return mapper.toResponse(user);
    }
    private AuthResponse newSession(User user) {
        AuthSession session = new AuthSession(); session.setUser(user);
        session.setExpiresAt(clock.instant().plus(properties.refreshTtl())); session.setLastUsedAt(clock.instant());
        sessions.save(session);
        return issuePair(user, session);
    }
    private AuthResponse issuePair(User user, AuthSession session) {
        String raw = SecretTokens.randomToken();
        RefreshToken token = new RefreshToken(); token.setSession(session); token.setTokenHash(SecretTokens.sha256(raw));
        token.setExpiresAt(session.getExpiresAt()); refreshTokens.save(token);
        var access = accessTokens.issue(user, session);
        return new AuthResponse("Bearer", access.value(), access.expiresAt(), raw, token.getExpiresAt(), mapper.toResponse(user));
    }
    private User lockActiveUser(UUID id) {
        User user = users.lockById(id).orElseThrow(() -> new ApiException(ErrorCode.AUTH_TOKEN_INVALID));
        if (user.getStatus() != UserStatus.ACTIVE) throw new ApiException(ErrorCode.AUTH_SESSION_REVOKED);
        return user;
    }
    private void revokeAllSessions(User user) {
        user.setTokenVersion(user.getTokenVersion() + 1);
        sessions.revokeAll(user.getId(), clock.instant());
    }
    private AuthChallenge createChallenge(User user, ChallengePurpose purpose, String secret, Duration ttl) {
        challenges.invalidate(user.getId(), purpose, clock.instant());
        AuthChallenge challenge = new AuthChallenge(); challenge.setUser(user); challenge.setPurpose(purpose);
        challenge.setSecretHash(challengeHash(user.getId(), purpose, secret)); challenge.setExpiresAt(clock.instant().plus(ttl));
        return challenges.saveAndFlush(challenge);
    }
    private void consumeChallenge(User user, UUID id, ChallengePurpose purpose, String secret) {
        AuthChallenge challenge = challenges.findById(id).orElseThrow(ChallengeRejectedException::new);
        if (!challenge.getUser().getId().equals(user.getId()) || challenge.getPurpose() != purpose
                || challenge.getConsumedAt() != null || !challenge.getExpiresAt().isAfter(clock.instant())
                || challenge.getAttempts() >= properties.maxChallengeAttempts()) throw new ChallengeRejectedException();
        if (!SecretTokens.equal(challenge.getSecretHash(), challengeHash(user.getId(), purpose, secret))) {
            challenge.setAttempts(challenge.getAttempts() + 1);
            if (challenge.getAttempts() >= properties.maxChallengeAttempts()) challenge.setConsumedAt(clock.instant());
            throw new ChallengeRejectedException();
        }
        challenge.setConsumedAt(clock.instant());
    }
    private String challengeHash(UUID userId, ChallengePurpose purpose, String secret) {
        return SecretTokens.hmac(pepper, "challenge:" + purpose + ":" + userId + ":" + secret);
    }
    private String normalizePhone(String phone) { String p=phone.trim().replaceAll("[\\s\\-()]", ""); if(p.startsWith("+91") && p.length()==13) p=p.substring(3); if(p.startsWith("91") && p.length()==12) p=p.substring(2); return p; }
    private String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
}
