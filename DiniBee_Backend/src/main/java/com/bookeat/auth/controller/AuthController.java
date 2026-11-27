package com.bookeat.auth.controller;
import com.bookeat.auth.request.*;
import com.bookeat.auth.response.*;
import com.bookeat.auth.service.AuthService;
import com.bookeat.common.response.ApiResponse;
import com.bookeat.security.AppPrincipal;
import com.bookeat.user.entity.RoleCode;
import com.bookeat.user.response.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/auth") @RequiredArgsConstructor
@Tag(name="01 Authentication", description="Register, authenticate, verify contacts and manage sessions")
@io.swagger.v3.oas.annotations.responses.ApiResponses({
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="400", description="Invalid request or verification challenge", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="401", description="Invalid credentials or expired/revoked session", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="403", description="Insufficient permission", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="409", description="Account conflict or already verified", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="429", description="Rate limit or resend cooldown", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class))),
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode="503", description="Phone verification provider is not configured", content=@io.swagger.v3.oas.annotations.media.Content(schema=@io.swagger.v3.oas.annotations.media.Schema(implementation=ApiResponse.class)))
})
public class AuthController {
    private final AuthService auth;

    @PostMapping("/register/customer") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create customer account", description="Role is fixed to CUSTOMER. Returns tokens and a safe user DTO.")
    public ApiResponse<AuthResponse> customer(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Customer account created", auth.register(request, RoleCode.CUSTOMER, http.getRemoteAddr()));
    }
    @PostMapping("/register/owner") @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary="Create owner account", description="Creates an owner profile. Restaurant application and approval are Phase 3.")
    public ApiResponse<AuthResponse> owner(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Owner account created", auth.register(request, RoleCode.OWNER, http.getRemoteAddr()));
    }
    @PostMapping("/login")
    @Operation(summary="Sign in with email and password", description="Same sign-in endpoint for all three clients. Invalid and inactive accounts use the same error.")
    public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Signed in", auth.login(request, http.getRemoteAddr()));
    }
    @PostMapping("/google")
    @Operation(summary="Sign in or register with Google OAuth", description="Validates Google account, creates customer account if new, returns auth tokens.")
    public ApiResponse<AuthResponse> google(@Valid @RequestBody GoogleLoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Signed in with Google", auth.googleLogin(request, http.getRemoteAddr()));
    }
    @PostMapping("/apple")
    @Operation(summary="Sign in or register with Apple ID", description="Validates Apple account, creates customer account if new, returns auth tokens.")
    public ApiResponse<AuthResponse> apple(@Valid @RequestBody AppleLoginRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Signed in with Apple", auth.appleLogin(request, http.getRemoteAddr()));
    }
    @PostMapping("/refresh-token")
    @Operation(summary="Rotate a refresh token", description="Single-use token. Reuse revokes its session. Client must serialize refresh requests.")
    public ApiResponse<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Token refreshed", auth.refresh(request, http.getRemoteAddr()));
    }
    @PostMapping("/logout") @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Revoke current session", description="Invalidates its access and refresh tokens immediately; other sessions remain active.")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AppPrincipal principal, HttpServletRequest http) {
        auth.logout(principal, http.getRemoteAddr()); return ApiResponse.ok("Signed out", null);
    }
    @PostMapping("/forgot-password") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary="Request password reset", description="Always returns the same accepted message for known and unknown addresses. Delivery is queued.")
    public ApiResponse<Void> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        auth.forgotPassword(request); return ApiResponse.ok("If the account is eligible, password reset instructions will be emailed", null);
    }
    @PostMapping("/reset-password")
    @Operation(summary="Reset password with an emailed token", description="Consumes token once and revokes every session. Sign in again afterwards.")
    public ApiResponse<Void> reset(@Valid @RequestBody ResetPasswordRequest request, HttpServletRequest http) {
        auth.resetPassword(request, http.getRemoteAddr()); return ApiResponse.ok("Password reset; sign in again", null);
    }
    @PostMapping("/change-password") @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Change password", description="Requires current password. Revokes every session including the current one.")
    public ApiResponse<Void> change(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody ChangePasswordRequest request, HttpServletRequest http) {
        auth.changePassword(principal.userId(), request, http.getRemoteAddr()); return ApiResponse.ok("Password changed; sign in again", null);
    }
    @PostMapping("/send-email-otp") @SecurityRequirement(name="bearerAuth") @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary="Queue email verification code", description="Sends to the signed-in user's email. Code expires in 10 minutes. Minimum resend interval is 60 seconds.")
    public ApiResponse<ChallengeResponse> sendEmail(@AuthenticationPrincipal AppPrincipal principal) {
        return ApiResponse.ok("Email verification queued", auth.sendEmailOtp(principal.userId()));
    }
    @PostMapping("/verify-email-otp") @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Verify own email", description="Single-use six-digit code; at most five incorrect attempts.")
    public ApiResponse<Void> verifyEmail(@AuthenticationPrincipal AppPrincipal principal, @Valid @RequestBody VerifyEmailRequest request, HttpServletRequest http) {
        auth.verifyEmailOtp(principal.userId(), request, http.getRemoteAddr()); return ApiResponse.ok("Email verified", null);
    }
    @PostMapping("/mobile-otp/send") @ResponseStatus(HttpStatus.ACCEPTED)
    public ApiResponse<ChallengeResponse> sendMobileOtp(@Valid @RequestBody MobileOtpSendRequest request) {
        return ApiResponse.ok("OTP sent", auth.sendMobileOtp(request));
    }
    @PostMapping("/mobile-otp/verify")
    public ApiResponse<AuthResponse> verifyMobileOtp(@Valid @RequestBody MobileOtpVerifyRequest request, HttpServletRequest http) {
        return ApiResponse.ok("Signed in", auth.verifyMobileOtp(request, http.getRemoteAddr()));
    }
    @GetMapping("/me") @SecurityRequirement(name="bearerAuth")
    @Operation(summary="Get own account", description="Returns current user, roles and permissions. No password hashes or provider credentials.")
    public ApiResponse<UserResponse> me(@AuthenticationPrincipal AppPrincipal principal) {
        return ApiResponse.ok("Account loaded", auth.me(principal.userId()));
    }
}
