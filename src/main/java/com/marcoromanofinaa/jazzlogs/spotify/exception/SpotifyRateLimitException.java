package com.marcoromanofinaa.jazzlogs.spotify.exception;

import java.util.Optional;

public class SpotifyRateLimitException extends SpotifyApiException {

    private final Optional<Integer> retryAfterSeconds;

    public SpotifyRateLimitException(String message, Optional<Integer> retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Optional<Integer> getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
