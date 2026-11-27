package com.bookeat.auth.request;
import jakarta.validation.constraints.*;
public record MobileOtpSendRequest(@NotBlank @Size(max=20) String phoneNumber, @NotBlank String role) {}
