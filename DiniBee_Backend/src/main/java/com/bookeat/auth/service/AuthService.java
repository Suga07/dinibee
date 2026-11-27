package com.bookeat.auth.service;
import com.bookeat.auth.request.*;
import com.bookeat.auth.response.*;
import com.bookeat.security.AppPrincipal;
import com.bookeat.user.entity.RoleCode;
import com.bookeat.user.response.UserResponse;
import java.util.UUID;

public interface AuthService {
    AuthResponse register(RegisterRequest request, RoleCode role, String ip);
    AuthResponse login(LoginRequest request, String ip);
    AuthResponse googleLogin(GoogleLoginRequest request, String ip);
    AuthResponse appleLogin(AppleLoginRequest request, String ip);
    AuthResponse refresh(RefreshRequest request, String ip);
    void logout(AppPrincipal principal, String ip);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request, String ip);
    void changePassword(UUID userId, ChangePasswordRequest request, String ip);
    ChallengeResponse sendEmailOtp(UUID userId);
    void verifyEmailOtp(UUID userId, VerifyEmailRequest request, String ip);
    ChallengeResponse sendMobileOtp(MobileOtpSendRequest request);
    AuthResponse verifyMobileOtp(MobileOtpVerifyRequest request, String ip);
    UserResponse me(UUID userId);
    UserResponse updateProfile(UUID userId, UpdateProfileRequest request, String ip);
}

