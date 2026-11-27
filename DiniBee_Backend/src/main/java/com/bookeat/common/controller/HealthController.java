package com.bookeat.common.controller;
import com.bookeat.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequiredArgsConstructor
@RequestMapping("/api/v1/public")
public class HealthController {
    private final JdbcTemplate jdbc;
    @Operation(summary = "Check API and database readiness")
    @ApiResponses({@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="200", description="Ready"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503", description="Database unavailable")})
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, String>>> health() {
        try {
            jdbc.queryForObject("select 1", Integer.class);
            return ResponseEntity.ok(ApiResponse.ok("Service is ready", Map.of("status", "UP", "database", "UP")));
        } catch (org.springframework.dao.DataAccessException ex) {
            return ResponseEntity.status(503).body(new ApiResponse<>(false, "Service is not ready",
                    Map.of("status", "DOWN"), "SERVICE_UNAVAILABLE", java.util.List.of(), java.time.Instant.now()));
        }
    }
}
