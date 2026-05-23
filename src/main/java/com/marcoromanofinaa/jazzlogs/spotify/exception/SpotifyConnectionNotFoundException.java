package com.marcoromanofinaa.jazzlogs.spotify.exception;

import java.util.UUID;

public class SpotifyConnectionNotFoundException extends RuntimeException {

    public SpotifyConnectionNotFoundException(UUID userId) {
        super("Spotify connection not found for user: " + userId);
    }

    public SpotifyConnectionNotFoundException(UUID userId, UUID spotifyConnectionId) {
        super("Spotify connection not found for user: " + userId + " and connection: " + spotifyConnectionId);
    }
}
