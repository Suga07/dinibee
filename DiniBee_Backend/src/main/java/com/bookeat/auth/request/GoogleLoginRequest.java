package com.bookeat.auth.request;

import jakarta.validation.constraints.Size;

public record GoogleLoginRequest(
    String idToken,
    @Size(max = 320) String email,
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    Boolean createIfMissing
) {
    public GoogleLoginRequest(String idToken, String email, String firstName, String lastName) {
        this(idToken, email, firstName, lastName, Boolean.TRUE);
    }

    @Override
    public String toString() {
        return "GoogleLoginRequest[email=" + email + ", idToken=" + (idToken != null ? "[PRESENT]" : "[NULL]") + ", createIfMissing=" + createIfMissing + "]";
    }
}
