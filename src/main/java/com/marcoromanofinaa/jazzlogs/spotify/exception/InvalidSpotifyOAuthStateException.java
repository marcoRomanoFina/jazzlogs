package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class InvalidSpotifyOAuthStateException extends RuntimeException {

    public InvalidSpotifyOAuthStateException(String state) {
        super("Invalid Spotify OAuth state: " + state);
    }
}
