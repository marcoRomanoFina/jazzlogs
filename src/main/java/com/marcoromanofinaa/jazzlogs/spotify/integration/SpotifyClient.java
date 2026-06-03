package com.marcoromanofinaa.jazzlogs.spotify.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyUserProfileDTO;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyApiException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyRateLimitException;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.SpotifyTimeRange;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyTopUserArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyUserTopTrackDTO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@RequiredArgsConstructor
public class SpotifyClient {

    private static final int PLAYLIST_PAGE_SIZE = 100;

    private final @Qualifier("spotifyRestClient") RestClient spotifyRestClient;
    private final ObjectMapper objectMapper;
    

    public SpotifyUserProfileDTO getCurrentUserProfile(String accessToken) {
        requireText(accessToken, "Spotify access token is required");

        try {
            var response = spotifyRestClient.get()
                    .uri("/me")
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .retrieve()
                    .body(SpotifyUserProfileDTO.class);

            if (response == null) {
                throw new SpotifyApiException("Spotify current user profile response was empty");
            }

            if (isBlank(response.spotifyUserId())) {
                throw new SpotifyApiException("Spotify current user profile response did not contain a Spotify user ID");
            }

            return response;
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException("fetch Spotify current user profile", exception);
        }
    }

    public List<SpotifyTopUserArtistDTO> getTopArtists(
            String accessToken,
            SpotifyTimeRange timeRange,
            int limit
    ) {
        requireText(accessToken, "Spotify access token is required");
        if (timeRange == null) {
            throw new IllegalArgumentException("Spotify time range is required");
        }
        requirePositive(limit, "Spotify top artists limit must be greater than zero");

        var uri = UriComponentsBuilder.fromPath("/me/top/artists")
                .queryParam("time_range", timeRange.apiValue())
                .queryParam("limit", limit)
                .build()
                .toUriString();

        try {
            var response = spotifyRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .retrieve()
                    .body(SpotifyTopArtistsResponse.class);

            if (response == null || response.items() == null) {
                throw new SpotifyApiException("Spotify top artists response was empty");
            }

            return response.items().stream()
                    .filter(artist -> artist != null && !isBlank(artist.spotifyArtistId()))
                    .map(artist -> new SpotifyTopUserArtistDTO(defaultText(artist.name())))
                    .toList();
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException("fetch Spotify top artists", exception);
        }
    }

    public List<SpotifyUserTopTrackDTO> getTopTracks(
            String accessToken,
            SpotifyTimeRange timeRange,
            int limit
    ) {
        requireText(accessToken, "Spotify access token is required");
        if (timeRange == null) {
            throw new IllegalArgumentException("Spotify time range is required");
        }
        requirePositive(limit, "Spotify top tracks limit must be greater than zero");

        var uri = UriComponentsBuilder.fromPath("/me/top/tracks")
                .queryParam("time_range", timeRange.apiValue())
                .queryParam("limit", limit)
                .build()
                .toUriString();

        try {
            var response = spotifyRestClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                    .retrieve()
                    .body(SpotifyTopTracksResponse.class);

            if (response == null || response.items() == null) {
                throw new SpotifyApiException("Spotify top tracks response was empty");
            }

            return response.items().stream()
                    .filter(track -> track != null && !isBlank(track.spotifyTrackId()))
                    .map(track -> new SpotifyUserTopTrackDTO(
                            defaultText(track.name()),
                            toArtistNames(track.artists()),
                            track.album() != null ? track.album().name() : null
                    ))
                    .toList();
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException("fetch Spotify top tracks", exception);
        }
    }

    public List<SpotifyPlaylistTrackDTO> getPlaylistTracks(
            String accessToken,
            String spotifyPlaylistId
    ) {
        requireText(accessToken, "Spotify access token is required");
        requireText(spotifyPlaylistId, "Spotify playlist ID is required");

        try {
            var tracks = new ArrayList<SpotifyPlaylistTrackDTO>();
            var visitedPageUris = new HashSet<String>();
            JsonNode response = getPlaylistFirstPage(accessToken, spotifyPlaylistId);

            while (true) {
                var items = extractPlaylistItems(response);
                if (!items.isArray()) {
                    throw new SpotifyApiException(
                            "Spotify playlist response did not include embedded track items. "
                                    + describePlaylistResponseShape(response)
                    );
                }

                for (JsonNode itemNode : items) {
                    if (isImportableTrackItem(itemNode)) {
                        tracks.add(toPlaylistTrack(extractTrackNode(itemNode)));
                    }
                }

                var nextPageUri = extractPlaylistNext(response);
                if (nextPageUri == null || nextPageUri.isBlank()) {
                    return tracks;
                }

                if (!visitedPageUris.add(nextPageUri)) {
                    throw new SpotifyApiException("Spotify playlist pagination loop detected for next page " + nextPageUri);
                }

                response = getPlaylistPage(accessToken, nextPageUri);
            }
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException("fetch Spotify playlist tracks", exception);
        }
    }

    public SpotifyPlaylistSummary getPlaylistSummary(
            String accessToken,
            String spotifyPlaylistId
    ) {
        requireText(accessToken, "Spotify access token is required");
        requireText(spotifyPlaylistId, "Spotify playlist ID is required");

        try {
            var response = getPlaylistFirstPage(accessToken, spotifyPlaylistId);

            if (response == null || response.isMissingNode()) {
                throw new SpotifyApiException("Spotify playlist summary response was empty");
            }

            return new SpotifyPlaylistSummary(
                    textOrNull(response, "id"),
                    defaultText(textOrNull(response, "name")),
                    textOrNull(response.path("owner"), "id"),
                    extractPlaylistTotal(response)
            );
        }
        catch (RestClientResponseException exception) {
            throw mapSpotifyException("fetch Spotify playlist summary", exception);
        }
    }

    private SpotifyPlaylistTrackDTO toPlaylistTrack(JsonNode track) {
        return new SpotifyPlaylistTrackDTO(
                textOrNull(track, "id"),
                defaultText(textOrNull(track, "name")),
                toArtistDtos(track.path("artists")),
                toAlbum(track.path("album")),
                intOrNull(track, "duration_ms"),
                intOrNull(track, "track_number"),
                textOrNull(track.path("external_urls"), "spotify")
        );
    }

    private SpotifyAlbumDTO toAlbum(JsonNode album) {
        if (album == null || album.isMissingNode() || album.isNull()) {
            return null;
        }

        return new SpotifyAlbumDTO(
                textOrNull(album, "id"),
                defaultText(textOrNull(album, "name")),
                toArtistDtos(album.path("artists")),
                textOrNull(album, "release_date"),
                intOrNull(album, "total_tracks"),
                firstImageUrl(album.path("images")).orElse(null),
                textOrNull(album.path("external_urls"), "spotify")
        );
    }

    private boolean isImportableTrackItem(JsonNode item) {
        if (item == null || item.isNull() || item.path("is_local").asBoolean(false)) {
            return false;
        }

        var track = extractTrackNode(item);
        var trackId = textOrNull(track, "id");
        return track != null
                && !track.isMissingNode()
                && "track".equals(textOrNull(track, "type"))
                && trackId != null
                && !trackId.isBlank();
    }

    private JsonNode getPlaylistFirstPage(String accessToken, String spotifyPlaylistId) {
        var uri = UriComponentsBuilder.fromPath("/playlists/{spotifyPlaylistId}")
                .queryParam("limit", PLAYLIST_PAGE_SIZE)
                .queryParam("offset", 0)
                .buildAndExpand(spotifyPlaylistId)
                .toUriString();

        return getPlaylistPage(accessToken, uri);
    }

    private JsonNode getPlaylistPage(String accessToken, String uri) {
        var response = spotifyRestClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                .retrieve()
                .body(String.class);

        if (response == null || response.isBlank()) {
            throw new SpotifyApiException("Spotify playlist response was empty");
        }

        try {
            return objectMapper.readTree(response);
        }
        catch (Exception exception) {
            throw new SpotifyApiException("Failed to parse Spotify playlist response", exception);
        }
    }

    private JsonNode extractPlaylistItems(JsonNode response) {
        var topLevelItems = response.path("items");
        if (topLevelItems.isArray()) {
            return topLevelItems;
        }

        var nestedTopLevelItems = topLevelItems.path("items");
        if (nestedTopLevelItems.isArray()) {
            return nestedTopLevelItems;
        }

        return response.path("tracks").path("items");
    }

    private Integer extractPlaylistTotal(JsonNode response) {
        var topLevelTotal = intOrNull(response, "total");
        if (topLevelTotal != null) {
            return topLevelTotal;
        }

        var topLevelItemsTotal = intOrNull(response.path("items"), "total");
        if (topLevelItemsTotal != null) {
            return topLevelItemsTotal;
        }

        return intOrNull(response.path("tracks"), "total");
    }

    private String extractPlaylistNext(JsonNode response) {
        var topLevelNext = textOrNull(response, "next");
        if (topLevelNext != null) {
            return topLevelNext;
        }

        var topLevelItemsNext = textOrNull(response.path("items"), "next");
        if (topLevelItemsNext != null) {
            return topLevelItemsNext;
        }

        return textOrNull(response.path("tracks"), "next");
    }

    private JsonNode extractTrackNode(JsonNode itemNode) {
        var trackNode = itemNode.path("item");
        if (!trackNode.isMissingNode() && !trackNode.isNull()) {
            return trackNode;
        }

        return itemNode.path("track");
    }

    private String describePlaylistResponseShape(JsonNode response) {
        if (response == null || response.isMissingNode() || response.isNull()) {
            return "Response body was empty after parsing.";
        }

        return "Top-level keys=" + fieldNamesOf(response)
                + ", tracks keys=" + fieldNamesOf(response.path("tracks"))
                + ", items node type=" + response.path("items").getNodeType()
                + ", tracks.items node type=" + response.path("tracks").path("items").getNodeType();
    }

    private List<String> fieldNamesOf(JsonNode node) {
        if (node == null || !node.isObject()) {
            return List.of();
        }

        var fieldNames = new ArrayList<String>();
        node.fieldNames().forEachRemaining(fieldNames::add);
        return fieldNames;
    }

    private Optional<String> firstImageUrl(JsonNode images) {
        if (images == null || !images.isArray()) {
            return Optional.empty();
        }

        for (JsonNode image : images) {
            var url = textOrNull(image, "url");
            if (url != null && !url.isBlank()) {
                return Optional.of(url);
            }
        }

        return Optional.empty();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private RuntimeException mapSpotifyException(String operation, RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 429) {
            var retryAfterSeconds = parseRetryAfterSeconds(
                    exception.getResponseHeaders() != null
                            ? exception.getResponseHeaders().getFirst("Retry-After")
                            : null
            );
            var message = retryAfterSeconds
                    .map(seconds -> "Spotify rate limit reached while trying to %s. Retry after %d seconds"
                            .formatted(operation, seconds))
                    .orElse("Spotify rate limit reached while trying to %s. Retry later.".formatted(operation));
            return new SpotifyRateLimitException(message, retryAfterSeconds);
        }

        var responseBody = exception.getResponseBodyAsString();
        var responseBodySuffix = (responseBody == null || responseBody.isBlank())
                ? ""
                : ". Response body: " + responseBody;

        return new SpotifyApiException(
                "Failed to %s. Spotify API responded with status %d%s"
                        .formatted(operation, exception.getStatusCode().value(), responseBodySuffix),
                exception
        );
    }

    private List<String> toArtistNames(List<SpotifyArtistSummary> artists) {
        if (artists == null || artists.isEmpty()) {
            return List.of();
        }

        return artists.stream()
                .filter(artist -> artist != null && !isBlank(artist.name()))
                .map(SpotifyArtistSummary::name)
                .toList();
    }

    private List<SpotifyArtistDTO> toArtistDtos(JsonNode artists) {
        if (artists == null || !artists.isArray() || artists.isEmpty()) {
            return List.of();
        }

        var artistDtos = new ArrayList<SpotifyArtistDTO>();
        for (JsonNode artist : artists) {
            var artistId = textOrNull(artist, "id");
            if (!isBlank(artistId)) {
                artistDtos.add(new SpotifyArtistDTO(
                        artistId,
                        defaultText(textOrNull(artist, "name")),
                        textOrNull(artist.path("external_urls"), "spotify")
                ));
            }
        }
        return artistDtos;
    }

    private String textOrNull(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        var valueNode = node.path(fieldName);
        if (valueNode.isMissingNode() || valueNode.isNull()) {
            return null;
        }

        var value = valueNode.asText();
        return value == null || value.isBlank() ? null : value;
    }

    private Integer intOrNull(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }

        var valueNode = node.path(fieldName);
        return valueNode.isIntegralNumber() ? valueNode.intValue() : null;
    }

    private void requireText(String value, String message) {
        if (isBlank(value)) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requirePositive(int value, String message) {
        if (value <= 0) {
            throw new IllegalArgumentException(message);
        }
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

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

    private record SpotifyTopArtistsResponse(List<SpotifyArtistSummary> items) {
    }

    private record SpotifyTopTracksResponse(List<SpotifyTopTrackResponse> items) {
    }

    public record SpotifyPlaylistSummary(
            String spotifyPlaylistId,
            String name,
            String ownerSpotifyUserId,
            Integer totalTracks
    ) {
    }

    private record SpotifyTopTrackResponse(
            @JsonProperty("id") String spotifyTrackId,
            String name,
            List<SpotifyArtistSummary> artists,
            SpotifyAlbumResponse album
    ) {
    }

    private record SpotifyAlbumResponse(
            @JsonProperty("id") String spotifyAlbumId,
            String name,
            List<SpotifyArtistSummary> artists,
            @JsonProperty("release_date") String releaseDate,
            @JsonProperty("total_tracks") Integer totalTracks,
            List<SpotifyImageResponse> images,
            @JsonProperty("external_urls") SpotifyExternalUrlsResponse externalUrls
    ) {
    }

    private record SpotifyArtistSummary(
            @JsonProperty("id") String spotifyArtistId,
            String name,
            @JsonProperty("external_urls") SpotifyExternalUrlsResponse externalUrls
    ) {
    }

    private record SpotifyExternalUrlsResponse(String spotify) {
    }

    private record SpotifyImageResponse(String url) {
    }
}
