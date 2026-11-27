package com.bookeat.auth.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 80) String firstName,
    @Size(max = 80) String lastName,
    @Email @Size(max = 320) String email,
    String password
) {}
