package com.marcoromanofinaa.jazzlogs.spotify.exception;

import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionStatus;
import java.util.UUID;

public class SpotifyConnectionNotConnectedException extends RuntimeException {

    public SpotifyConnectionNotConnectedException(UUID userId, SpotifyConnectionStatus status) {
        super("Spotify connection for user " + userId + " is not connected. Current status: " + status);
    }
}
