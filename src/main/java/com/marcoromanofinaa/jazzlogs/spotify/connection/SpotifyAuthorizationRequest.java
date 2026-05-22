package com.marcoromanofinaa.jazzlogs.spotify.connection;

import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Set;

public record SpotifyAuthorizationRequest(
        @NotEmpty(message = "At least one Spotify scope is required")
        Set<@NotNull SpotifyScope> scopes
) {
}
