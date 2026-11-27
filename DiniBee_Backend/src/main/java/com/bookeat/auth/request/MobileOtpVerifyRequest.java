package com.bookeat.auth.request;
import jakarta.validation.constraints.*;
import java.util.UUID;
public record MobileOtpVerifyRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp="\\d{4}") String code) {}
