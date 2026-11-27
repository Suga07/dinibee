package com.bookeat.admin.controller;
import com.bookeat.auth.request.UpdateProfileRequest;
import com.bookeat.auth.service.AuthService;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.security.AppPrincipal;
import com.bookeat.user.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/admin")
@SecurityRequirement(name="bearerAuth")
public class AdminProfileController {
    private final AuthService auth;

    @GetMapping("/profile") @PreAuthorize("hasAuthority('ADMIN_SELF_READ') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'SUPPORT_ADMIN', 'RESTAURANT_VERIFICATION_ADMIN', 'OPERATIONS_ADMIN')")
    @Operation(summary="Get own administrator profile", description="Requires an administrator role.")
    public ApiResponse<UserResponse> profile(@AuthenticationPrincipal AppPrincipal principal) {
        return ApiResponse.ok("Administrator profile loaded", auth.me(principal.userId()));
    }

    @PutMapping("/profile") @PreAuthorize("hasAuthority('ADMIN_SELF_READ') or hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FINANCE_ADMIN', 'SUPPORT_ADMIN', 'RESTAURANT_VERIFICATION_ADMIN', 'OPERATIONS_ADMIN')")
    @Operation(summary="Update own administrator profile", description="Updates administrator profile details such as name, email and password.")
    public ApiResponse<UserResponse> updateProfile(
            @AuthenticationPrincipal AppPrincipal principal,
            @Valid @RequestBody UpdateProfileRequest request,
            HttpServletRequest http) {
        return ApiResponse.ok("Administrator profile updated successfully", auth.updateProfile(principal.userId(), request, http.getRemoteAddr()));
    }
}

