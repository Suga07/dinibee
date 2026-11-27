package com.bookeat.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, String message, T data,
                             String errorCode, List<ValidationError> errors, Instant timestamp) {
    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(true, message, data, null, null, Instant.now());
    }
    public static ApiResponse<Void> error(String code, String message, List<ValidationError> errors) {
        return new ApiResponse<>(false, message, null, code, errors, Instant.now());
    }
    public record ValidationError(String field, String message) {}
}
