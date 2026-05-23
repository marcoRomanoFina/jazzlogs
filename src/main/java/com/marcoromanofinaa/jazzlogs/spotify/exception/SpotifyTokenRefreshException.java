package com.marcoromanofinaa.jazzlogs.spotify.exception;

import java.util.Optional;
import java.util.UUID;

public class SpotifyTokenRefreshException extends RuntimeException {

    private final Optional<Integer> retryAfterSeconds;

    public SpotifyTokenRefreshException(UUID userId, UUID spotifyConnectionId) {
        super("Failed to refresh Spotify access token for user " + userId + " and connection " + spotifyConnectionId);
        this.retryAfterSeconds = Optional.empty();
    }

    public SpotifyTokenRefreshException(UUID userId, UUID spotifyConnectionId, Throwable cause) {
        super("Failed to refresh Spotify access token for user " + userId + " and connection " + spotifyConnectionId, cause);
        this.retryAfterSeconds = Optional.empty();
    }

    public SpotifyTokenRefreshException(String message) {
        super(message);
        this.retryAfterSeconds = Optional.empty();
    }

    public SpotifyTokenRefreshException(String message, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = Optional.empty();
    }

    public SpotifyTokenRefreshException(String message, Optional<Integer> retryAfterSeconds, Throwable cause) {
        super(message, cause);
        this.retryAfterSeconds = retryAfterSeconds == null ? Optional.empty() : retryAfterSeconds;
    }

    public Optional<Integer> getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
