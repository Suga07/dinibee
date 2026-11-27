package com.bookeat.auth.response;
import java.time.Instant;
import java.util.UUID;
public record ChallengeResponse(UUID challengeId, Instant expiresAt) {}
