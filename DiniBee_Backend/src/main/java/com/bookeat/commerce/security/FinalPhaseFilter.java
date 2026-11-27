package com.bookeat.commerce.security;

import com.bookeat.exception.ApiException;
import com.bookeat.exception.ErrorCode;
import com.bookeat.security.AppPrincipal;
import com.bookeat.security.JsonSecurityErrors;
import com.bookeat.security.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;

@RequiredArgsConstructor
public class FinalPhaseFilter extends OncePerRequestFilter {

    private final RateLimitService limits;
    private final JsonSecurityErrors errors;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String path = request.getRequestURI().substring(request.getContextPath().length());
        String actor = request.getRemoteAddr();
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppPrincipal p) {
            actor = p.userId().toString();
        }

        String scope = null;
        int limit = 20;
        boolean write = !request.getMethod().equals("GET") && !request.getMethod().equals("OPTIONS");

        if      (path.startsWith("/api/v1/customer/payments") && write) { scope = "payment-attempt"; }
        else if (path.contains("/coupons/") && write)                   { scope = "coupon-attempt"; }
        else if (path.contains("/support/") && write)                   { scope = "support-write";  limit = 15; }
        else if (path.contains("/check-in") && write)                   { scope = "check-in";       limit = 10; }
        else if (path.contains("/rewards/") && write)                   { scope = "reward-write"; }
        else if (path.endsWith("/reviews") && write)                    { scope = "review-write";   limit = 10; }
        else if (path.contains("/reports/"))                            { scope = "report-read";    limit = 30; }
        else if (path.equals("/api/v1/payments/webhooks/zoho") ||
                 path.equals("/api/v1/payments/razorpay/webhook"))     { scope = "gateway-webhook"; limit = 300; }

        if (scope != null) {
            try {
                limits.consume(scope, actor, limit, Duration.ofMinutes(1));
            } catch (ApiException ex) {
                errors.write(response, ex.getCode());
                return;
            }
        }

        if (write && (path.equals("/api/v1/payments/webhooks/zoho") ||
                      path.equals("/api/v1/payments/razorpay/webhook") || isJson(request.getContentType()))) {
            int max = path.contains("/webhook") ? 262_144 : 1_048_576;
            byte[] body = request.getInputStream().readNBytes(max + 1);
            if (body.length > max) {
                errors.write(response, ErrorCode.PAYLOAD_TOO_LARGE);
                return;
            }
            // BodyCachingRequestWrapper and CachedBodyInputStream live in separate
            // top-level .java files in this package — no inner/anonymous classes,
            // so hot-reload can never produce ClassNotFoundException for them.
            chain.doFilter(new BodyCachingRequestWrapper(request, body), response);
            return;
        }

        chain.doFilter(request, response);
    }

    private static boolean isJson(String contentType) {
        if (contentType == null) return false;
        String type = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return type.equals("application/json") || (type.startsWith("application/") && type.endsWith("+json"));
    }
}
