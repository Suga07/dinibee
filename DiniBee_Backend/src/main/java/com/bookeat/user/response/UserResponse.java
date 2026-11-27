package com.bookeat.user.response;
import com.bookeat.user.entity.UserStatus;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserResponse(UUID id, String firstName, String lastName, String email,
        String phoneNumber, UserStatus status, boolean emailVerified,
        Set<String> roles, Set<String> permissions, Instant createdAt) {}
