package com.bookeat.customer.controller;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.customer.dto.CustomerProfileUpdateRequest;
import com.bookeat.security.AppPrincipal;
import com.bookeat.user.response.ProfileResponse;
import com.bookeat.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customer")
@SecurityRequirement(name="bearerAuth")
public class CustomerProfileController {
    private final ProfileService profiles;

    @GetMapping("/profile")
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    @Operation(summary="Get own customer profile", description="Uses authenticated identity. No arbitrary user ID is accepted.")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal AppPrincipal principal) {
        return ApiResponse.ok("Profile loaded", profiles.customer(principal.userId()));
    }

    @PutMapping("/profile")
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    @Operation(summary="Update own customer profile", description="Updates authenticated customer name, phone and email. Changing email resets email verification.")
    public ApiResponse<ProfileResponse> updateProfile(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody CustomerProfileUpdateRequest request) {
        return ApiResponse.ok("Profile updated", profiles.updateCustomer(principal.userId(), request));
    }

    @DeleteMapping("/profile")
    @PreAuthorize("hasAuthority('CUSTOMER_SELF_READ')")
    @Operation(summary="Delete own customer account", description="Permanently deactivates authenticated customer account and deletes customer profile.")
    public ApiResponse<Void> deleteProfile(@AuthenticationPrincipal AppPrincipal principal) {
        profiles.deleteCustomer(principal.userId());
        return ApiResponse.ok("Account deleted successfully", null);
    }
}
