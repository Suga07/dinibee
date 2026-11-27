package com.bookeat.user.response;
import java.util.UUID;
public record ProfileResponse(UUID profileId, UserResponse user) {}
