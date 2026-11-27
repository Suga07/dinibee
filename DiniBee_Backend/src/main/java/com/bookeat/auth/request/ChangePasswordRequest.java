package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record ChangePasswordRequest(@ValidPassword String currentPassword, @ValidPassword String newPassword) {
    @Override public String toString() { return "ChangePasswordRequest[redacted]"; }
}
