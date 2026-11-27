package com.bookeat.auth.request;

import jakarta.validation.constraints.Size;

public record AppleLoginRequest(
    String identityToken,
    String authorizationCode,
    @Size(max = 320) String email,
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    String userIdentifier
) {
    @Override
    public String toString() {
        return "AppleLoginRequest[email=" + email + ", userIdentifier=" + userIdentifier + ", identityToken=" + (identityToken != null ? "[PRESENT]" : "[NULL]") + "]";
    }
}
