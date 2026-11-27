package com.bookeat.exception;
/** Commits the attempt/revocation recorded before this exception is raised. */
public class ChallengeRejectedException extends ApiException {
    public ChallengeRejectedException() { super(ErrorCode.AUTH_CHALLENGE_INVALID); }
}
