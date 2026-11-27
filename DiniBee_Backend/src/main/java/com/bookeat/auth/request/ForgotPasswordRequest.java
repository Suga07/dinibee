package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ForgotPasswordRequest(@NotBlank @Email @Size(max=320) String email) {
    @Override public String toString() { return "ForgotPasswordRequest[redacted]"; }
}
