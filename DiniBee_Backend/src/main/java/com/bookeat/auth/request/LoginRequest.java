package com.bookeat.auth.request;
import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank @Size(max=320) String email, @NotBlank @Size(max=256) String password) {
    @Override public String toString() { return "LoginRequest[redacted]"; }
}
