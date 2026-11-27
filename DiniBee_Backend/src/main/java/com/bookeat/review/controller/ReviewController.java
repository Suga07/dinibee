package com.bookeat.review.controller;

import com.bookeat.common.response.*;
import com.bookeat.review.dto.*;
import com.bookeat.review.service.ReviewService;
import com.bookeat.security.AppPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/** Public, owner and admin review routes. Customer routes live in
 * {@link CustomerReviewController} so /api/v1/customer/reviews has one clear
 * class-level mapping and cannot disappear behind mixed route registration. */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class ReviewController {
    private final ReviewService service;

    @GetMapping("/api/v1/restaurants/{restaurantId}/reviews")
    @Operation(summary = "List visible public restaurant reviews")
    public ApiResponse<PageResponse<ReviewResponse>> publicReviews(
            @PathVariable UUID restaurantId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.ok("Reviews loaded", service.publicReviews(restaurantId, page, size));
    }

    @PostMapping("/api/v1/owner/restaurants/{restaurantId}/reviews/{id}/reply")
    @PreAuthorize("hasRole('OWNER') and hasAuthority('RESTAURANT_OWN_MANAGE')")
    public ApiResponse<ReviewResponse> reply(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID restaurantId,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequests.Reply request) {
        return ApiResponse.ok("Review reply saved", service.reply(principal.userId(), restaurantId, id, request));
    }

    @PutMapping("/api/v1/admin/reviews/{id}/hide")
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    public ApiResponse<ReviewResponse> hide(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequests.Action request) {
        return ApiResponse.ok("Review hidden", service.moderate(principal.userId(), id, request, true));
    }

    @PutMapping("/api/v1/admin/reviews/{id}/restore")
    @PreAuthorize("hasAuthority('REVIEW_MODERATE')")
    public ApiResponse<ReviewResponse> restore(
            @AuthenticationPrincipal AppPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ReviewRequests.Action request) {
        return ApiResponse.ok("Review restored", service.moderate(principal.userId(), id, request, false));
    }
}
