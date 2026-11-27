package com.bookeat.admin.service;
import com.bookeat.common.audit.AuditService;
import com.bookeat.common.validation.PasswordRules;
import com.bookeat.user.entity.*;
import com.bookeat.user.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service @RequiredArgsConstructor @Slf4j
public class AdminBootstrapService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwords;
    private final JdbcTemplate jdbc;
    private final AuditService audit;
    @Transactional
    public void createFirstAdmin(String email, String password) {
        jdbc.execute("select pg_advisory_xact_lock(72641531)");
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length() > 320
                || !PasswordRules.valid(password) || password.length() < 20)
            throw new IllegalArgumentException("Bootstrap requires a valid email and a password of 20+ characters within 72 UTF-8 bytes");
        String normalized = email.trim().toLowerCase(Locale.ROOT);
        User user = users.findByEmail(normalized).orElseGet(() -> {
            User u = new User();
            u.setEmail(normalized);
            u.setFirstName("Platform");
            u.setLastName("Administrator");
            return u;
        });
        user.setPasswordHash(passwords.encode(password));
        for (RoleCode role : new RoleCode[]{RoleCode.ADMIN, RoleCode.SUPER_ADMIN}) {
            var r = roles.findByCode(role).orElseThrow();
            if (!user.getRoles().contains(r)) {
                user.getRoles().add(r);
            }
        }
        users.saveAndFlush(user);
        audit.record(user.getId(), "SUPER_ADMIN_BOOTSTRAPPED", "USER", user.getId(), null);
        log.info("Administrator account synced for {}", normalized);
    }
}
