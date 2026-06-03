package com.marcoromanofinaa.jazzlogs.spotify.exception;

import java.util.UUID;

public class SpotifyAccountAlreadyLinkedException extends RuntimeException {

    public SpotifyAccountAlreadyLinkedException(String spotifyUserId, UUID existingUserId) {
        super("Spotify account " + spotifyUserId + " is already linked to another Jazzlogs user: " + existingUserId);
    }
}
