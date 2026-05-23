package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class SpotifyTokenExchangeException extends RuntimeException {

    public SpotifyTokenExchangeException(String message) {
        super(message);
    }

    public SpotifyTokenExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
