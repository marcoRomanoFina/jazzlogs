package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class SpotifyPlaylistSyncException extends RuntimeException {

    public SpotifyPlaylistSyncException(String message) {
        super(message);
    }

    public SpotifyPlaylistSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
