package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class SpotifyAuthorizationUrlBuilder {

    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";

    private final SpotifyProperties spotifyProperties;

    public SpotifyAuthorizationUrlBuilder(SpotifyProperties spotifyProperties) {
        this.spotifyProperties = spotifyProperties;
    }

    public String build(String state, Set<SpotifyScope> scopes) {
        ensureConfiguredOAuthClient();

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", spotifyProperties.oauth().clientId())
                .queryParam("scope", String.join(" ", scopes.stream().map(SpotifyScope::value).sorted().toList()))
                .queryParam("redirect_uri", spotifyProperties.oauth().redirectUri())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    private void ensureConfiguredOAuthClient() {
        requireText(spotifyProperties.oauth().clientId(), "Spotify client ID is not configured");
        requireText(spotifyProperties.oauth().redirectUri(), "Spotify redirect URI is not configured");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }
}
