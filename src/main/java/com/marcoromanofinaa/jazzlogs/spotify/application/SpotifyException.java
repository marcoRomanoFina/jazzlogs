package com.marcoromanofinaa.jazzlogs.spotify.application;

public class SpotifyException extends RuntimeException {

    private final int statusCode;

    public SpotifyException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public SpotifyException(String message) {
        this(500, message);
    }

    public int getStatusCode() {
        return statusCode;
    }
}
