package com.bookeat.security;
import com.bookeat.exception.ApiException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;

@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Set<String> PATHS = Set.of("register/customer", "register/owner", "login",
            "refresh-token", "logout", "forgot-password", "reset-password", "change-password",
            "send-email-otp", "verify-email-otp");
    private final RateLimitService limiter;
    private final JsonSecurityErrors errors;
    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        String prefix = "/api/v1/auth/";
        return !"POST".equals(request.getMethod()) || !path.startsWith(prefix)
                || !PATHS.contains(path.substring(prefix.length()));
    }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            String path = request.getServletPath();
            boolean refresh = path.endsWith("/refresh-token");
            int limit = refresh ? 120 : path.contains("register") ? 10 : 30;
            // Forwarded headers are not trusted. Configure an edge limit when deployed behind a proxy.
            limiter.consume("ip:" + path, request.getRemoteAddr(), limit,
                    refresh ? Duration.ofMinutes(1) : Duration.ofMinutes(15));
        } catch (ApiException ex) { errors.write(response, ex.getCode()); return; }
        chain.doFilter(request, response);
    }
}
