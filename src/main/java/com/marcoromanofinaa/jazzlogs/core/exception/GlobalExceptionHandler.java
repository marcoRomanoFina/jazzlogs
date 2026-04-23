package com.marcoromanofinaa.jazzlogs.core.exception;

import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyRateLimitException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApplicationException.class)
    public ResponseEntity<ApiErrorResponse> handleApplicationException(ApplicationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyRateLimit(SpotifyRateLimitException exception) {
        var responseBuilder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);

        exception.getRetryAfterSeconds()
                .ifPresent(seconds -> responseBuilder.header("Retry-After", String.valueOf(seconds)));

        return responseBuilder.body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        var message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> "%s %s".formatted(error.getField(), error.getDefaultMessage()))
                .orElse("Request validation failed");

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(message));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException exception) {
        var message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse("Request validation failed");

        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(message));
    }

}
