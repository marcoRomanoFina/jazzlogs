package com.marcoromanofinaa.jazzlogs.core.exception;

import com.marcoromanofinaa.jazzlogs.spotify.exception.ConsumedSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.ExpiredSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.InvalidOfficialSpotifyOwnerException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.InvalidSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyAccountAlreadyLinkedException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyApiException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyCatalogImportException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotConnectedException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotFoundException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionOwnershipException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyMissingScopesException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyPlaylistSyncException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyRateLimitException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTasteSnapshotSyncException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenExchangeException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenRefreshException;
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

    @ExceptionHandler({
            InvalidSpotifyOAuthStateException.class,
            ExpiredSpotifyOAuthStateException.class,
            ConsumedSpotifyOAuthStateException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyConnectionOwnershipException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyConnectionOwnership(SpotifyConnectionOwnershipException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyConnectionNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyConnectionNotFound(SpotifyConnectionNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            SpotifyAccountAlreadyLinkedException.class,
            SpotifyConnectionNotConnectedException.class,
            SpotifyMissingScopesException.class,
            InvalidOfficialSpotifyOwnerException.class
    })
    public ResponseEntity<ApiErrorResponse> handleSpotifyConflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyTokenRefreshException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyTokenRefresh(SpotifyTokenRefreshException exception) {
        if (exception.getRetryAfterSeconds().isPresent()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header("Retry-After", String.valueOf(exception.getRetryAfterSeconds().get()))
                    .body(new ApiErrorResponse(exception.getMessage()));
        }

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyTokenExchangeException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyTokenExchange(SpotifyTokenExchangeException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyRateLimitException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyRateLimit(SpotifyRateLimitException exception) {
        var responseBuilder = ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE);

        exception.getRetryAfterSeconds()
                .ifPresent(seconds -> responseBuilder.header("Retry-After", String.valueOf(seconds)));

        return responseBuilder.body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(SpotifyApiException.class)
    public ResponseEntity<ApiErrorResponse> handleSpotifyApi(SpotifyApiException exception) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ApiErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            SpotifyPlaylistSyncException.class,
            SpotifyTasteSnapshotSyncException.class,
            SpotifyCatalogImportException.class,
            IllegalStateException.class
    })
    public ResponseEntity<ApiErrorResponse> handleSpotifyInternal(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(exception.getMessage()));
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
