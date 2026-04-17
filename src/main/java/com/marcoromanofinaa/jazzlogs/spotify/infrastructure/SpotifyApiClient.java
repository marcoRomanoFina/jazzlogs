package com.marcoromanofinaa.jazzlogs.spotify.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyException;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyRateLimitException;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyProperties;
import java.net.URI;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
// Thin HTTP client for Spotify. It owns the raw API concerns of the integration:
// build the OAuth URL, exchange and refresh tokens, fetch playlist pages, and
// translate Spotify HTTP failures into application-level exceptions.
public class SpotifyApiClient {

    private static final String AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
    private static final String TOKEN_URL = "https://accounts.spotify.com/api/token";
    private static final String PLAYLIST_ITEMS_URL = "https://api.spotify.com/v1/playlists/{playlistId}/items";
    private static final String PLAYLIST_SCOPE = "playlist-read-private";

    private final SpotifyProperties spotifyProperties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient = RestClient.builder().build();

    // Builds the user-facing Spotify authorization URL for the Authorization
    // Code flow, including the scope and backend-issued OAuth state.
    public String buildAuthorizationUrl(String state) {
        ensureConfiguredClientCredentials();

        return UriComponentsBuilder.fromUriString(AUTHORIZE_URL)
                .queryParam("response_type", "code")
                .queryParam("client_id", spotifyProperties.clientId())
                .queryParam("scope", PLAYLIST_SCOPE)
                .queryParam("redirect_uri", spotifyProperties.redirectUri())
                .queryParam("state", state)
                .build()
                .toUriString();
    }

    // Exchanges the short-lived authorization code returned by Spotify for the
    // access/refresh token pair we persist in spotify_connections.
    public SpotifyTokenResponse exchangeAuthorizationCode(String code) {
        ensureConfiguredClientCredentials();

        return postForm(
                TOKEN_URL,
                formData(
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", spotifyProperties.redirectUri(),
                        "client_id", spotifyProperties.clientId(),
                        "client_secret", spotifyProperties.clientSecret()
                ),
                SpotifyTokenResponse.class,
                "Spotify authorization code response was empty"
        );
    }

    // Uses the stored refresh token to request a new access token once the
    // existing bearer token expires.
    public SpotifyTokenResponse refreshAccessToken(String refreshToken) {
        ensureConfiguredClientCredentials();

        return postForm(
                TOKEN_URL,
                formData(
                        "grant_type", "refresh_token",
                        "refresh_token", refreshToken,
                        "client_id", spotifyProperties.clientId(),
                        "client_secret", spotifyProperties.clientSecret()
                ),
                SpotifyTokenResponse.class,
                "Spotify refresh token response was empty"
        );
    }

    // Fetches one page of playlist items using the narrowed field projection we
    // need for local album/artist/track normalization.
    public PlaylistItemsPage fetchPlaylistItems(
            String accessToken,
            String playlistId,
            String market,
            int limit,
            int offset
    ) {
        var uriBuilder = UriComponentsBuilder.fromUriString(PLAYLIST_ITEMS_URL)
                .queryParam("limit", limit)
                .queryParam("offset", offset)
                .queryParam("fields", "items(added_at,is_local,item(type,id,name,duration_ms,disc_number,track_number,external_urls,album(id,name,album_type,total_tracks,release_date,release_date_precision,external_urls,images),artists(id,name,type,uri,href,external_urls))),next,total,limit,offset");

        if (market != null && !market.isBlank()) {
            uriBuilder.queryParam("market", market);
        }

        URI uri = uriBuilder.buildAndExpand(playlistId).toUri();
        var response = readJson(
                getRequiredBody(uri, accessToken, "Spotify playlist response was empty"),
                "Spotify playlist response"
        );

        return new PlaylistItemsPage(
                response.path("items"),
                response.path("next").asText(null),
                response.path("total").asInt()
        );
    }

    // Parses a raw JSON body into a tree so the sync layer can extract only the
    // nested fields it cares about.
    private JsonNode readJson(String responseBody, String responseLabel) {
        try {
            return objectMapper.readTree(responseBody);
        }
        catch (Exception exception) {
            throw new SpotifyException("Failed to parse %s".formatted(responseLabel));
        }
    }

    // Shared GET helper for Spotify bearer-authenticated endpoints that should
    // never return an empty response body.
    private String getRequiredBody(URI uri, String accessToken, String emptyBodyMessage) {
        try {
            var responseBody = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                    .retrieve()
                    .body(String.class);

            return requireText(responseBody, emptyBodyMessage, 500);
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException(exception);
        }
    }

    // Shared form POST helper used by both the authorization-code exchange and
    // the refresh-token exchange.
    private <T> T postForm(
            String uri,
            LinkedMultiValueMap<String, String> form,
            Class<T> responseType,
            String emptyBodyMessage
    ) {
        try {
            var response = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new SpotifyException(emptyBodyMessage);
            }

            return response;
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException(exception);
        }
    }

    // Small helper for `application/x-www-form-urlencoded` requests so the
    // token methods stay linear and readable.
    private LinkedMultiValueMap<String, String> formData(String... entries) {
        var form = new LinkedMultiValueMap<String, String>();
        for (int index = 0; index < entries.length; index += 2) {
            form.add(entries[index], entries[index + 1]);
        }
        return form;
    }

    // Converts Spotify HTTP errors into domain exceptions, with a specialized
    // branch for 429 responses so callers can surface Retry-After guidance.
    private SpotifyException mapSpotifyException(RestClientResponseException exception) {
        var statusCode = exception.getStatusCode().value();
        var defaultMessage = "Spotify API request failed with status %d".formatted(statusCode);

        if (statusCode == 429) {
            var retryAfterSeconds = parseRetryAfterSeconds(
                    exception.getResponseHeaders() != null
                            ? exception.getResponseHeaders().getFirst("Retry-After")
                            : null
            );
            var message = retryAfterSeconds
                    .map(seconds -> "Spotify rate limit reached. Retry after %d seconds".formatted(seconds))
                    .orElse("Spotify rate limit reached. Retry later.");

            return new SpotifyRateLimitException(message, retryAfterSeconds);
        }

        try {
            var root = objectMapper.readTree(exception.getResponseBodyAsString());
            if (root.has("error")) {
                var errorNode = root.path("error");
                if (errorNode.isTextual()) {
                    return new SpotifyException(statusCode, errorNode.asText(defaultMessage));
                }

                var message = errorNode.path("message").asText(defaultMessage);
                return new SpotifyException(statusCode, message);
            }
        }
        catch (Exception ignored) {
        }

        return new SpotifyException(statusCode, defaultMessage);
    }

    // Parses Spotify's Retry-After header when present so rate-limit handling
    // can expose how long the caller should wait before retrying.
    private Optional<Integer> parseRetryAfterSeconds(String retryAfterHeader) {
        if (retryAfterHeader == null || retryAfterHeader.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(retryAfterHeader));
        }
        catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }

    // Fails fast when the core OAuth client settings are missing from config.
    private void ensureConfiguredClientCredentials() {
        requireText(spotifyProperties.clientId(), "Spotify client ID is not configured", 500);
        requireText(spotifyProperties.clientSecret(), "Spotify client secret is not configured", 500);
        requireText(spotifyProperties.redirectUri(), "Spotify redirect URI is not configured", 500);
    }

    // Centralizes the "required non-blank text" check used by both config and
    // HTTP response validation paths.
    private String requireText(String value, String message, int statusCode) {
        if (value == null || value.isBlank()) {
            throw new SpotifyException(statusCode, message);
        }
        return value;
    }

    public record PlaylistItemsPage(JsonNode items, String next, int total) {
    }
}
