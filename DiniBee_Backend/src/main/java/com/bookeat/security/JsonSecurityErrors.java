package com.bookeat.security;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.List;

@Component @RequiredArgsConstructor
public class JsonSecurityErrors {
    private final ObjectMapper mapper;
    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus().value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        if (code.getStatus().value() == 401) response.setHeader("WWW-Authenticate", "Bearer");
        if (code == ErrorCode.RATE_LIMITED) response.setHeader("Retry-After", "900");
        mapper.writeValue(response.getOutputStream(), ApiResponse.error(code.name(), code.getMessage(), List.of()));
    }
}
