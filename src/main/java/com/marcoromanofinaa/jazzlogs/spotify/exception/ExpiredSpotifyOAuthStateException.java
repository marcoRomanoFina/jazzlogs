package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class ExpiredSpotifyOAuthStateException extends RuntimeException {

    public ExpiredSpotifyOAuthStateException(String state) {
        super("Expired Spotify OAuth state: " + state);
    }
}
