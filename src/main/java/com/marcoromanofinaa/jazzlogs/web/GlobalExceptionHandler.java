package com.marcoromanofinaa.jazzlogs.web;

import com.marcoromanofinaa.jazzlogs.logbook.application.AlbumLogNotFoundException;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyException;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyRateLimitException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SpotifyRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyRateLimit(SpotifyRateLimitException exception) {
        var responseBuilder = ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS);

        exception.getRetryAfterSeconds()
                .ifPresent(seconds -> responseBuilder.header("Retry-After", String.valueOf(seconds)));

        return responseBuilder.body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyException(SpotifyException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(AlbumLogNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleAlbumLogNotFound(AlbumLogNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException exception) {
        return ResponseEntity.status(exception.getStatusCode())
                .body(new ApiErrorResponse(exception.getReason()));
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
