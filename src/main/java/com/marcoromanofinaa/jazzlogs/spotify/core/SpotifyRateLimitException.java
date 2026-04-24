package com.marcoromanofinaa.jazzlogs.spotify.core;

import java.util.Optional;

public class SpotifyRateLimitException extends SpotifyException {

    private final Optional<Integer> retryAfterSeconds;

    public SpotifyRateLimitException(String message, Optional<Integer> retryAfterSeconds) {
        super(429, message);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public Optional<Integer> getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
