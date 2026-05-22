package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class ConsumedSpotifyOAuthStateException extends RuntimeException {

    public ConsumedSpotifyOAuthStateException(String state) {
        super("Spotify OAuth state was already consumed: " + state);
    }
}
