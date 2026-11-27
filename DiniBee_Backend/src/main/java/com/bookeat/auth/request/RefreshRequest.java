package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record RefreshRequest(@NotBlank @Pattern(regexp="^[A-Za-z0-9_-]{43}$") String refreshToken) {
    @Override public String toString() { return "RefreshRequest[redacted]"; }
}
