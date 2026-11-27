package com.bookeat.security;
import java.util.UUID;
public record AppPrincipal(UUID userId, UUID sessionId) {}
