package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ResetPasswordRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{43}$") String token,
        @ValidPassword String newPassword) {
    @Override public String toString() { return "ResetPasswordRequest[redacted]"; }
}
