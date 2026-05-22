package com.marcoromanofinaa.jazzlogs.spotify.exception;

public class SpotifyTasteSnapshotSyncException extends RuntimeException {

    public SpotifyTasteSnapshotSyncException(String message) {
        super(message);
    }

    public SpotifyTasteSnapshotSyncException(String message, Throwable cause) {
        super(message, cause);
    }
}
