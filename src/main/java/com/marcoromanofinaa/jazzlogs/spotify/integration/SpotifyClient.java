package com.marcoromanofinaa.jazzlogs.spotify.integration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
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

    private static final int DEFAULT_PLAYLIST_PAGE_SIZE = 50;
    private static final int MAX_PLAYLIST_PAGE_COUNT = 1_000;

    private final @Qualifier("spotifyRestClient") RestClient spotifyRestClient;
    private final SpotifyProperties spotifyProperties;

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
                    .map(artist -> new SpotifyTopUserArtistDTO(artist.spotifyArtistId(), defaultText(artist.name())))
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
                            track.spotifyTrackId(),
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

        var importedTracks = new ArrayList<SpotifyPlaylistTrackDTO>();
        var limit = resolvePlaylistPageSize();

        for (int offset = 0, page = 0; ; offset += limit, page++) {
            if (page >= MAX_PLAYLIST_PAGE_COUNT) {
                throw new SpotifyApiException(
                        "Spotify playlist pagination exceeded the maximum number of pages for playlist " + spotifyPlaylistId
                );
            }

            var uriBuilder = UriComponentsBuilder.fromPath("/playlists/{spotifyPlaylistId}/tracks")
                    .queryParam("limit", limit)
                    .queryParam("offset", offset);

            if (spotifyProperties.sync().market() != null && !spotifyProperties.sync().market().isBlank()) {
                uriBuilder.queryParam("market", spotifyProperties.sync().market());
            }

            try {
                var response = spotifyRestClient.get()
                        .uri(uriBuilder.buildAndExpand(spotifyPlaylistId).toUriString())
                        .header(HttpHeaders.AUTHORIZATION, bearerToken(accessToken))
                        .retrieve()
                        .body(SpotifyPlaylistTracksResponse.class);

                if (response == null || response.items() == null) {
                    throw new SpotifyApiException("Spotify playlist tracks response was empty");
                }

                var pageItems = response.items();

                pageItems.stream()
                        .filter(this::isImportableTrackItem)
                        .map(SpotifyPlaylistItemResponse::track)
                        .map(this::toPlaylistTrack)
                        .forEach(importedTracks::add);

                if (pageItems.isEmpty() || response.next() == null || response.next().isBlank()) {
                    break;
                }
            }
            catch (RestClientResponseException exception) {
                throw mapSpotifyException("fetch Spotify playlist tracks", exception);
            }
        }

        return importedTracks;
    }

    private SpotifyPlaylistTrackDTO toPlaylistTrack(SpotifyPlaylistTrackResponse track) {
        return new SpotifyPlaylistTrackDTO(
                track.spotifyTrackId(),
                defaultText(track.name()),
                toArtistDtos(track.artists()),
                toAlbum(track.album()),
                track.durationMs(),
                track.trackNumber(),
                track.externalUrls() != null ? track.externalUrls().spotify() : null
        );
    }

    private SpotifyAlbumDTO toAlbum(SpotifyAlbumResponse album) {
        if (album == null) {
            return null;
        }

        return new SpotifyAlbumDTO(
                album.spotifyAlbumId(),
                defaultText(album.name()),
                toArtistDtos(album.artists()),
                album.releaseDate(),
                album.totalTracks(),
                firstImageUrl(album.images()).orElse(null),
                album.externalUrls() != null ? album.externalUrls().spotify() : null
        );
    }

    private boolean isImportableTrackItem(SpotifyPlaylistItemResponse item) {
        if (item == null || item.isLocal()) {
            return false;
        }

        var track = item.track();
        return track != null
                && "track".equals(track.type())
                && track.spotifyTrackId() != null
                && !track.spotifyTrackId().isBlank();
    }

    private Optional<String> firstImageUrl(List<SpotifyImageResponse> images) {
        if (images == null) {
            return Optional.empty();
        }

        return images.stream()
                .map(SpotifyImageResponse::url)
                .filter(url -> url != null && !url.isBlank())
                .findFirst();
    }

    private String bearerToken(String accessToken) {
        return "Bearer " + accessToken;
    }

    private int resolvePlaylistPageSize() {
        return spotifyProperties.sync().pageSize() > 0
                ? spotifyProperties.sync().pageSize()
                : DEFAULT_PLAYLIST_PAGE_SIZE;
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

        return new SpotifyApiException(
                "Failed to %s. Spotify API responded with status %d"
                        .formatted(operation, exception.getStatusCode().value()),
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

    private List<SpotifyArtistDTO> toArtistDtos(List<SpotifyArtistSummary> artists) {
        if (artists == null || artists.isEmpty()) {
            return List.of();
        }

        return artists.stream()
                .filter(artist -> artist != null && !isBlank(artist.spotifyArtistId()))
                .map(artist -> new SpotifyArtistDTO(
                        artist.spotifyArtistId(),
                        defaultText(artist.name()),
                        artist.externalUrls() != null ? artist.externalUrls().spotify() : null
                ))
                .toList();
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

    private record SpotifyPlaylistTracksResponse(
            List<SpotifyPlaylistItemResponse> items,
            String next
    ) {
    }

    private record SpotifyPlaylistItemResponse(
            @JsonProperty("is_local") boolean isLocal,
            @JsonProperty("track") SpotifyPlaylistTrackResponse track
    ) {
    }

    private record SpotifyPlaylistTrackResponse(
            @JsonProperty("id") String spotifyTrackId,
            String type,
            String name,
            @JsonProperty("artists") List<SpotifyArtistSummary> artists,
            SpotifyAlbumResponse album,
            @JsonProperty("duration_ms") Integer durationMs,
            @JsonProperty("track_number") Integer trackNumber,
            @JsonProperty("external_urls") SpotifyExternalUrlsResponse externalUrls
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
