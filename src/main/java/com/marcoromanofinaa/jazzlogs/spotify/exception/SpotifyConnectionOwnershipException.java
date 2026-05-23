package com.marcoromanofinaa.jazzlogs.spotify.exception;

import java.util.UUID;

public class SpotifyConnectionOwnershipException extends RuntimeException {

    public SpotifyConnectionOwnershipException(UUID userId, UUID spotifyConnectionId) {
        super("Spotify connection " + spotifyConnectionId + " does not belong to user: " + userId);
    }
}
