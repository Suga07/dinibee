package com.bookeat.commerce.security;
import com.bookeat.security.*;
import com.bookeat.exception.ApiException;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;
import lombok.RequiredArgsConstructor;
import java.io.IOException;
import java.time.Duration;
@RequiredArgsConstructor
public class CommerceRateLimitFilter extends OncePerRequestFilter {
    private final RateLimitService limiter;private final JsonSecurityErrors errors;
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException {
        String path=request.getRequestURI().substring(request.getContextPath().length());String scope=null;int limit=60;String identity=request.getRemoteAddr();
        var auth=SecurityContextHolder.getContext().getAuthentication();if(auth!=null&&auth.getPrincipal() instanceof AppPrincipal p)identity=p.userId().toString();
        if("GET".equals(request.getMethod())&&path.startsWith("/api/v1/restaurants")&&!path.endsWith("/availability")){scope="discovery";identity=request.getRemoteAddr();}
        else if("POST".equals(request.getMethod())&&(path.equals("/api/v1/customer/dinitickets")||path.matches("/api/v1/owner/restaurants/[^/]+/dinitickets"))){scope="ticket-create";limit=10;}
        else if("POST".equals(request.getMethod())&&path.contains("/join/")){scope="booking-invite-join";limit=15;}
        else if("POST".equals(request.getMethod())&&path.matches("/api/v1/customer/(reservations|group-bookings|corporate-bookings|orders|takeaway/orders)")){scope="commerce-create";limit=30;}
        else if("PUT".equals(request.getMethod())&&path.endsWith("/location")){scope="location-update";limit=30;}
        else if(path.endsWith("/stream")||path.endsWith("/events")){scope="queue-events";limit=60;}
        if(scope!=null)try{limiter.consume(scope,identity,limit,Duration.ofMinutes(1));}catch(ApiException e){errors.write(response,e.getCode());return;}
        chain.doFilter(request,response);
    }
}
