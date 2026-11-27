package com.bookeat.owner.controller;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.security.AppPrincipal;
import com.bookeat.user.response.ProfileResponse;
import com.bookeat.user.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/owner")
@SecurityRequirement(name="bearerAuth")
public class OwnerProfileController {
    private final ProfileService profiles;
    @GetMapping("/profile") @PreAuthorize("hasAuthority('OWNER_SELF_READ')")
    @Operation(summary="Get own owner profile", description="Uses authenticated identity. No arbitrary user ID is accepted.")
    public ApiResponse<ProfileResponse> profile(@AuthenticationPrincipal AppPrincipal principal) {
        return ApiResponse.ok("Profile loaded", profiles.owner(principal.userId()));
    }
    @PutMapping("/profile") @PreAuthorize("hasAuthority('OWNER_SELF_READ')")
    @Operation(summary="Update own owner profile", description="Updates user details.")
    public ApiResponse<ProfileResponse> updateProfile(@AuthenticationPrincipal AppPrincipal principal, @RequestBody @jakarta.validation.Valid com.bookeat.user.request.UpdateProfileRequest req) {
        return ApiResponse.ok("Profile updated", profiles.updateOwner(principal.userId(), req));
    }
}
