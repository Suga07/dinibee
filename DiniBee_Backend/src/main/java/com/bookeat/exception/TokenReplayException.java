package com.bookeat.exception;
/** Commits the attempt/revocation recorded before this exception is raised. */
public class TokenReplayException extends ApiException {
    public TokenReplayException() { super(ErrorCode.AUTH_REFRESH_REUSED); }
}
