package com.bookeat.review.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.PageResponse;
import com.bookeat.review.dto.ReviewRequests;
import com.bookeat.review.dto.ReviewResponse;
import com.bookeat.review.service.ReviewService;
import com.bookeat.security.AppPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer/reviews")
@PreAuthorize("hasRole('CUSTOMER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Customer reviews")
public class CustomerReviewController {
    private final ReviewService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create review for a completed reservation or food order", description = "Idempotency-Key required. Customer only; review eligibility checked. Awards review points if eligible.")
    public ApiResponse<ReviewResponse> create(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReviewRequests.Create request) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : UUID.randomUUID().toString();
        return ApiResponse.ok("Review created", service.create(principal.userId(), key, request));
    }

    @PostMapping("/submit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Submit review alias for frontend compatibility")
    public ApiResponse<ReviewResponse> submit(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ReviewRequests.Create request) {
        String key = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey : UUID.randomUUID().toString();
        return ApiResponse.ok("Review created", service.create(principal.userId(), key, request));
    }

    @GetMapping
    @Operation(summary = "List the signed-in customer's persisted reviews")
    public ApiResponse<PageResponse<ReviewResponse>> list(
            @AuthenticationPrincipal AppPrincipal principal,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Reviews loaded", service.own(principal.userId(), page, size));
    }

    @PutMapping("/{id}")
    public ApiResponse<ReviewResponse> update(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequests.Update request) {
        return ApiResponse.ok("Review updated", service.update(principal.userId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequests.Action request) {
        service.delete(principal.userId(), id, request);
        return ApiResponse.ok("Review deleted", null);
    }
}
