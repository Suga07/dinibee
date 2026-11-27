package com.bookeat.config;
import com.bookeat.exception.ErrorCode;
import com.bookeat.security.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.*;
import java.util.*;

@Configuration @EnableMethodSecurity
public class SecurityConfig {
    @Bean public CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins:}") String origins) {
        var allowed = Arrays.stream(origins.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
        CorsConfiguration cors = new CorsConfiguration();
        List<String> patterns = new ArrayList<>(allowed);
        patterns.add("http://localhost:*");
        patterns.add("http://127.0.0.1:*");
        cors.setAllowedOriginPatterns(patterns);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("*"));
        cors.setExposedHeaders(List.of("Retry-After"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
    @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource cors,
            DatabaseJwtAuthenticationConverter converter, JsonSecurityErrors errors, RateLimitService limiter,
            @Value("${app.docs-public:false}") boolean docsPublic) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable) // Bearer header authentication; no ambient cookies.
            .formLogin(AbstractHttpConfigurer::disable).httpBasic(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable)
            .cors(c -> c.configurationSource(cors))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> {
                a.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll();
                a.requestMatchers(HttpMethod.GET, "/api/v1/public/health", "/api/v1/public/media/**", "/api/v1/public/restaurants/**", "/actuator/health", "/actuator/health/**").permitAll();
                a.requestMatchers(HttpMethod.GET, "/api/v1/restaurants/*/availability", "/api/v1/restaurants/*/availability/seat-summary", "/api/v1/restaurants", "/api/v1/restaurants/search", "/api/v1/restaurants/*", "/api/v1/restaurants/*/menu", "/api/v1/restaurants/*/operating-hours", "/api/v1/restaurants/*/reviews", "/api/v1/membership-plans", "/api/v1/public/platform-stats").permitAll();
                a.requestMatchers(HttpMethod.POST, "/api/v1/payments/webhooks/zoho").permitAll();
                a.requestMatchers(HttpMethod.POST, "/api/v1/payments/razorpay/webhook").permitAll();
                a.requestMatchers(HttpMethod.POST, "/api/v1/auth/register/customer", "/api/v1/auth/register/owner",
                        "/api/v1/auth/login", "/api/v1/auth/google", "/api/v1/auth/apple", "/api/v1/auth/refresh-token", "/api/v1/auth/forgot-password",
                        "/api/v1/auth/reset-password", "/api/v1/auth/mobile-otp/**", "/api/v1/owner/auth/**", "/api/v1/owner/onboarding").permitAll();
                a.requestMatchers(HttpMethod.GET, "/api/v1/owner/auth/**").permitAll();
                if (docsPublic) a.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll();
                else a.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").hasAnyRole("ADMIN", "SUPER_ADMIN");
                a.requestMatchers("/api/v1/customer/**").hasRole("CUSTOMER");
                a.requestMatchers("/api/v1/owner/**").hasAnyRole("OWNER", "RESTAURANT_MANAGER");
                a.requestMatchers("/api/v1/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN", "FINANCE_ADMIN", "SUPPORT_ADMIN",
                        "RESTAURANT_VERIFICATION_ADMIN", "OPERATIONS_ADMIN", "CONTENT_ADMIN");
                a.anyRequest().authenticated();
            })
            .oauth2ResourceServer(o -> o.jwt(j -> j.jwtAuthenticationConverter(converter))
                    .authenticationEntryPoint((r, s, e) -> errors.write(s, ErrorCode.AUTH_TOKEN_INVALID))
                    .accessDeniedHandler((r, s, e) -> errors.write(s, ErrorCode.ACCESS_DENIED)))
            .exceptionHandling(e -> e.authenticationEntryPoint((r, s, ex) -> errors.write(s, ErrorCode.AUTH_TOKEN_INVALID))
                    .accessDeniedHandler((r, s, ex) -> errors.write(s, ErrorCode.ACCESS_DENIED)))
            .addFilterBefore(new RateLimitFilter(limiter, errors), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new com.bookeat.availability.security.AvailabilityRateLimitFilter(limiter, errors), RateLimitFilter.class)
            .addFilterAfter(new com.bookeat.commerce.security.CommerceRateLimitFilter(limiter, errors), BearerTokenAuthenticationFilter.class)
            .addFilterAfter(new com.bookeat.commerce.security.FinalPhaseFilter(limiter, errors), com.bookeat.commerce.security.CommerceRateLimitFilter.class);
        return http.build();
    }
}
