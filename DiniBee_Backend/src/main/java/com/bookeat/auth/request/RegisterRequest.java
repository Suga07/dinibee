package com.bookeat.auth.request;
import com.bookeat.common.validation.ValidPassword;
import jakarta.validation.constraints.*;
import java.util.UUID;

public record RegisterRequest(@NotBlank @Size(max=80) String firstName, @NotBlank @Size(max=80) String lastName,
        @NotBlank @Email @Size(max=320) String email, @ValidPassword String password,
        @Pattern(regexp="^\\+[1-9][0-9]{7,14}$", message="Use E.164 format, for example +919876543210") String phoneNumber) {
    @Override public String toString() { return "RegisterRequest[redacted]"; }
}
