package com.bookeat.availability.security;
import com.bookeat.security.*;
import com.bookeat.exception.ApiException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.time.Duration;
@RequiredArgsConstructor
public class AvailabilityRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService limiter;
    private final JsonSecurityErrors errors;
    @Override protected boolean shouldNotFilter(HttpServletRequest r){return !"GET".equals(r.getMethod()) || !r.getServletPath().matches("/api/v1/restaurants/[^/]+/availability");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws IOException,ServletException {
        try{limiter.consume("availability",request.getRemoteAddr(),60,Duration.ofMinutes(1));}
        catch(ApiException ex){errors.write(response,ex.getCode());return;}
        chain.doFilter(request,response);
    }
}
