package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record VerifyEmailRequest(@NotNull UUID challengeId, @NotBlank @Pattern(regexp="^[0-9]{6}$") String code) {
    @Override public String toString() { return "VerifyEmailRequest[redacted]"; }
}
