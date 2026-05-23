package com.marcoromanofinaa.jazzlogs.spotify.exception;

import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import java.util.Set;
import java.util.UUID;

public class SpotifyMissingScopesException extends RuntimeException {

    public SpotifyMissingScopesException(UUID userId, Set<SpotifyScope> requiredScopes) {
        super("Spotify connection for user " + userId + " is missing required scopes: " + requiredScopes);
    }
}
