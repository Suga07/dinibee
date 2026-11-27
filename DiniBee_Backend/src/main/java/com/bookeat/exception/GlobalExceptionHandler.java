package com.bookeat.exception;

import com.bookeat.common.response.ApiResponse;
import com.bookeat.common.response.ApiResponse.ValidationError;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import java.util.List;
import java.util.UUID;

@Slf4j @RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(com.bookeat.restaurant.service.OnboardingIncompleteException.class)
    public ResponseEntity<ApiResponse<Void>> onboarding(com.bookeat.restaurant.service.OnboardingIncompleteException ex) {
        var errors=ex.getSteps().stream().map(step->new ValidationError(step.name(), "Required step is incomplete")).toList();
        return ResponseEntity.unprocessableEntity().body(ApiResponse.error(ex.getCode().name(),ex.getMessage(),errors));
    }
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> tooLarge() {
        return ResponseEntity.status(413).body(ApiResponse.error("DOCUMENT_TOO_LARGE","Maximum upload size is 5 MiB",List.of()));
    }
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> api(ApiException ex) {
        return error(ex.getCode());
    }
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> validation(MethodArgumentNotValidException ex) {
        var errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ValidationError(e.getField(), e.getDefaultMessage())).toList();
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_FAILED", "Request validation failed", errors));
    }
    @ExceptionHandler({HttpMessageNotReadableException.class, ConstraintViolationException.class,
            HandlerMethodValidationException.class, MethodArgumentTypeMismatchException.class,
            org.springframework.web.multipart.support.MissingServletRequestPartException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class,
            org.springframework.web.bind.MissingRequestHeaderException.class})
    public ResponseEntity<ApiResponse<Void>> malformed(Exception ex) { return error(ErrorCode.VALIDATION_FAILED); }
    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ResponseEntity<ApiResponse<Void>> conflict(Exception ex) { return error(ErrorCode.CONFLICT); }
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> forbidden() { return error(ErrorCode.ACCESS_DENIED); }
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Void>> missing() { return error(ErrorCode.RESOURCE_NOT_FOUND); }
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> method() {
        return ResponseEntity.status(405).body(ApiResponse.error("METHOD_NOT_ALLOWED", "Method is not supported", List.of()));
    }
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> media() {
        return ResponseEntity.status(415).body(ApiResponse.error("UNSUPPORTED_MEDIA_TYPE", "Use the content type documented for this endpoint", List.of()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> unexpected(Exception ex) {
        log.error("Unhandled failure reference={} type={} msg={}", UUID.randomUUID(), ex.getClass().getSimpleName(), ex.getMessage(), ex);
        return error(ErrorCode.INTERNAL_ERROR);
    }
    private ResponseEntity<ApiResponse<Void>> error(ErrorCode code) {
        return ResponseEntity.status(code.getStatus()).body(ApiResponse.error(code.name(), code.getMessage(), List.of()));
    }
}
