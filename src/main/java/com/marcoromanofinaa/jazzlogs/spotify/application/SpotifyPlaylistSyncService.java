package com.marcoromanofinaa.jazzlogs.spotify.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbumSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyArtistSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyTrackSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyApiClient;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyArtistRepository;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyTrackRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// Syncs the canonical Spotify playlist into local normalized tables. The sync
// now behaves like a delta reconciliation: fetch Spotify's current snapshot,
// compare it with the database snapshot, and apply only inserts, updates, and
// removals instead of wiping everything each run.
public class SpotifyPlaylistSyncService {

    private static final int PAGE_SIZE = 50;

    private final SpotifyProperties spotifyProperties;
    private final SpotifyConnectionService spotifyConnectionService;
    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyAlbumRepository spotifyAlbumRepository;
    private final SpotifyArtistRepository spotifyArtistRepository;
    private final SpotifyTrackRepository spotifyTrackRepository;
    private final SpotifyAlbumLogBindingService spotifyAlbumLogBindingService;

    @Transactional
    // Main sync entrypoint: page through the playlist, collect albums/artists/
    // tracks in memory, reconcile them against existing rows, persist the delta,
    // then bind any unlinked editorial logs to the synced Spotify albums.
    public int syncConfiguredPlaylist() {
        var playlistId = requireConfiguredPlaylistId();

        var accessToken = spotifyConnectionService.getValidConnection().getAccessToken();
        var albumsById = new LinkedHashMap<String, SpotifyAlbum>();
        var artistsById = new LinkedHashMap<String, SpotifyArtist>();
        var tracksById = new LinkedHashMap<String, SpotifyTrack>();
        var total = Integer.MAX_VALUE;

        for (var offset = 0; offset < total; ) {
            var playlistPage = spotifyApiClient.fetchPlaylistItems(
                    accessToken,
                    playlistId,
                    spotifyProperties.market(),
                    PAGE_SIZE,
                    offset
            );

            collectAlbumsTracksAndArtists(
                    albumsById,
                    artistsById,
                    tracksById,
                    playlistPage.items()
            );

            total = playlistPage.total();
            offset += playlistPage.items().size();
        }

        var existingAlbumsById = spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc(playlistId).stream()
                .collect(java.util.stream.Collectors.toMap(SpotifyAlbum::getSpotifyAlbumId, album -> album, (left, right) -> left, LinkedHashMap::new));
        var existingArtistsById = spotifyArtistRepository.findAll().stream()
                .collect(java.util.stream.Collectors.toMap(SpotifyArtist::getSpotifyArtistId, artist -> artist, (left, right) -> left, LinkedHashMap::new));
        var existingTracksById = spotifyTrackRepository.findAllBySourcePlaylistId(playlistId).stream()
                .collect(java.util.stream.Collectors.toMap(SpotifyTrack::getSpotifyTrackId, track -> track, (left, right) -> left, LinkedHashMap::new));

        var syncedAlbumsById = reconcileAlbums(playlistId, albumsById, existingAlbumsById);
        var syncedArtistsById = reconcileArtists(artistsById, existingArtistsById);
        var syncedTracksById = reconcileTracks(playlistId, tracksById, existingTracksById, syncedAlbumsById, syncedArtistsById);

        spotifyAlbumRepository.saveAll(syncedAlbumsById.values());
        spotifyArtistRepository.saveAll(syncedArtistsById.values());
        spotifyTrackRepository.saveAll(syncedTracksById.values());
        spotifyTrackRepository.flush();
        deleteRemovedTracks(existingTracksById, syncedTracksById);
        deleteRemovedAlbums(existingAlbumsById, syncedAlbumsById);
        deleteRemovedArtists(existingArtistsById, syncedArtistsById);
        spotifyAlbumLogBindingService.bindConfiguredPlaylistAlbumsToLogs();
        return tracksById.size();
    }

    // Reuses existing album rows when possible and only updates the fields that
    // are considered part of the synced Spotify snapshot.
    private Map<String, SpotifyAlbum> reconcileAlbums(
            String playlistId,
            Map<String, SpotifyAlbum> fetchedAlbumsById,
            Map<String, SpotifyAlbum> existingAlbumsById
    ) {
        var syncedAlbumsById = new LinkedHashMap<String, SpotifyAlbum>();

        for (var fetchedAlbum : fetchedAlbumsById.values()) {
            var existingAlbum = existingAlbumsById.get(fetchedAlbum.getSpotifyAlbumId());
            if (existingAlbum == null) {
                syncedAlbumsById.put(fetchedAlbum.getSpotifyAlbumId(), fetchedAlbum);
                continue;
            }

            existingAlbum.updateSyncData(new SpotifyAlbumSyncData(
                    playlistId,
                    fetchedAlbum.getName(),
                    fetchedAlbum.getSpotifyUrl(),
                    fetchedAlbum.getCoverImageUrl(),
                    fetchedAlbum.getAlbumType(),
                    fetchedAlbum.getTotalTracks(),
                    fetchedAlbum.getReleaseDate(),
                    fetchedAlbum.getReleaseDatePrecision()
            ));
            syncedAlbumsById.put(existingAlbum.getSpotifyAlbumId(), existingAlbum);
        }

        return syncedAlbumsById;
    }

    // Artists are global Spotify entities, so reconciliation happens by Spotify
    // artist id instead of by playlist membership.
    private Map<String, SpotifyArtist> reconcileArtists(
            Map<String, SpotifyArtist> fetchedArtistsById,
            Map<String, SpotifyArtist> existingArtistsById
    ) {
        var syncedArtistsById = new LinkedHashMap<String, SpotifyArtist>();

        for (var fetchedArtist : fetchedArtistsById.values()) {
            var existingArtist = existingArtistsById.get(fetchedArtist.getSpotifyArtistId());
            if (existingArtist == null) {
                syncedArtistsById.put(fetchedArtist.getSpotifyArtistId(), fetchedArtist);
                continue;
            }

            existingArtist.updateSyncData(new SpotifyArtistSyncData(
                    fetchedArtist.getName(),
                    fetchedArtist.getSpotifyUrl(),
                    fetchedArtist.getHref(),
                    fetchedArtist.getUri(),
                    fetchedArtist.getType()
            ));
            syncedArtistsById.put(existingArtist.getSpotifyArtistId(), existingArtist);
        }

        return syncedArtistsById;
    }

    // Tracks are the richest sync object because they must be rewired to the
    // reconciled album and artist instances before being saved back to the DB.
    private Map<String, SpotifyTrack> reconcileTracks(
            String playlistId,
            Map<String, SpotifyTrack> fetchedTracksById,
            Map<String, SpotifyTrack> existingTracksById,
            Map<String, SpotifyAlbum> syncedAlbumsById,
            Map<String, SpotifyArtist> syncedArtistsById
    ) {
        var syncedTracksById = new LinkedHashMap<String, SpotifyTrack>();

        for (var fetchedTrack : fetchedTracksById.values()) {
            var album = Optional.ofNullable(fetchedTrack.getAlbum())
                    .map(SpotifyAlbum::getSpotifyAlbumId)
                    .map(syncedAlbumsById::get)
                    .orElse(null);
            var mainArtist = Optional.ofNullable(fetchedTrack.getMainArtist())
                    .map(SpotifyArtist::getSpotifyArtistId)
                    .map(syncedArtistsById::get)
                    .orElse(null);
            var secondaryArtists = fetchedTrack.getSecondaryArtists().stream()
                    .map(SpotifyArtist::getSpotifyArtistId)
                    .map(syncedArtistsById::get)
                    .filter(java.util.Objects::nonNull)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

            var existingTrack = existingTracksById.get(fetchedTrack.getSpotifyTrackId());
            if (existingTrack == null) {
                fetchedTrack.updateSyncData(new SpotifyTrackSyncData(
                        playlistId,
                        album,
                        mainArtist,
                        secondaryArtists,
                        fetchedTrack.getName(),
                        fetchedTrack.getArtistNames(),
                        fetchedTrack.getSpotifyUrl(),
                        fetchedTrack.getDurationMs(),
                        fetchedTrack.getDiscNumber(),
                        fetchedTrack.getTrackNumber(),
                        fetchedTrack.getAddedToPlaylistAt()
                ));
                syncedTracksById.put(fetchedTrack.getSpotifyTrackId(), fetchedTrack);
                continue;
            }

            existingTrack.updateSyncData(new SpotifyTrackSyncData(
                    playlistId,
                    album,
                    mainArtist,
                    secondaryArtists,
                    fetchedTrack.getName(),
                    fetchedTrack.getArtistNames(),
                    fetchedTrack.getSpotifyUrl(),
                    fetchedTrack.getDurationMs(),
                    fetchedTrack.getDiscNumber(),
                    fetchedTrack.getTrackNumber(),
                    fetchedTrack.getAddedToPlaylistAt()
            ));
            syncedTracksById.put(existingTrack.getSpotifyTrackId(), existingTrack);
        }

        return syncedTracksById;
    }

    // Deletes tracks that used to belong to the playlist snapshot but are no
    // longer returned by Spotify.
    private void deleteRemovedTracks(
            Map<String, SpotifyTrack> existingTracksById,
            Map<String, SpotifyTrack> syncedTracksById
    ) {
        var removedTracks = existingTracksById.entrySet().stream()
                .filter(entry -> !syncedTracksById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedTracks.isEmpty()) {
            spotifyTrackRepository.deleteAll(removedTracks);
            spotifyTrackRepository.flush();
        }
    }

    // Deletes playlist albums that disappeared from the latest Spotify snapshot.
    private void deleteRemovedAlbums(
            Map<String, SpotifyAlbum> existingAlbumsById,
            Map<String, SpotifyAlbum> syncedAlbumsById
    ) {
        var removedAlbums = existingAlbumsById.entrySet().stream()
                .filter(entry -> !syncedAlbumsById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedAlbums.isEmpty()) {
            spotifyAlbumRepository.deleteAll(removedAlbums);
            spotifyAlbumRepository.flush();
        }
    }

    // Deletes artists that are no longer referenced by the synced playlist
    // snapshot after tracks and albums were reconciled.
    private void deleteRemovedArtists(
            Map<String, SpotifyArtist> existingArtistsById,
            Map<String, SpotifyArtist> syncedArtistsById
    ) {
        var removedArtists = existingArtistsById.entrySet().stream()
                .filter(entry -> !syncedArtistsById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedArtists.isEmpty()) {
            spotifyArtistRepository.deleteAll(removedArtists);
            spotifyArtistRepository.flush();
        }
    }

    // Walks the raw playlist JSON page and builds in-memory entities keyed by
    // Spotify ids. This gives the reconciliation phase a clean local snapshot
    // without persisting partial rows page by page.
    private void collectAlbumsTracksAndArtists(
            Map<String, SpotifyAlbum> albumsById,
            Map<String, SpotifyArtist> artistsById,
            Map<String, SpotifyTrack> tracksById,
            JsonNode items
    ) {
        if (items == null || !items.isArray()) {
            return;
        }

        for (var playlistItemNode : items) {
            if (playlistItemNode.path("is_local").asBoolean(false)) {
                continue;
            }

            var trackNode = resolveTrackNode(playlistItemNode);
            if (trackNode == null || trackNode.isMissingNode() || trackNode.isNull()) {
                continue;
            }

            if (!"track".equals(trackNode.path("type").asText())) {
                continue;
            }

            var spotifyTrackId = readText(trackNode, "id");
            if (spotifyTrackId.isEmpty()) {
                continue;
            }

            var album = toAlbum(trackNode.path("album"))
                    .map(parsedAlbum -> {
                        albumsById.putIfAbsent(parsedAlbum.getSpotifyAlbumId(), parsedAlbum);
                        return albumsById.get(parsedAlbum.getSpotifyAlbumId());
                    })
                    .orElse(null);

            var trackArtists = resolveArtists(artistsById, trackNode.path("artists"));

            var track = tracksById.get(spotifyTrackId.get());
            if (track == null) {
                track = toTrack(playlistItemNode, trackNode, album, trackArtists);
                tracksById.put(spotifyTrackId.get(), track);
            }
        }
    }

    // Spotify can return the playlist item payload under `item` in the fields
    // projection we request, but this fallback keeps the parser tolerant.
    private JsonNode resolveTrackNode(JsonNode playlistItemNode) {
        if (playlistItemNode.hasNonNull("item")) {
            return playlistItemNode.path("item");
        }
        return playlistItemNode.path("track");
    }

    // Converts the nested Spotify album JSON object into the normalized album
    // entity used by the local catalog.
    private Optional<SpotifyAlbum> toAlbum(JsonNode albumNode) {
        return readText(albumNode, "id")
                .map(spotifyAlbumId -> SpotifyAlbum.builder()
                        .spotifyAlbumId(spotifyAlbumId)
                        .sourcePlaylistId(spotifyProperties.playlistId())
                        .name(albumNode.path("name").asText(""))
                        .spotifyUrl(readText(albumNode.path("external_urls"), "spotify").orElse(null))
                        .coverImageUrl(extractCoverImageUrl(albumNode.path("images")).orElse(null))
                        .albumType(readText(albumNode, "album_type").orElse(null))
                        .totalTracks(albumNode.hasNonNull("total_tracks") ? albumNode.path("total_tracks").asInt() : null)
                        .releaseDate(readText(albumNode, "release_date").orElse(null))
                        .releaseDatePrecision(readText(albumNode, "release_date_precision").orElse(null))
                        .build());
    }

    private SpotifyTrack toTrack(
            JsonNode playlistItemNode,
            JsonNode trackNode,
            SpotifyAlbum album,
            List<SpotifyArtist> trackArtists
    ) {
        // Spotify returns artists ordered, so the first one becomes the main
        // artist and the rest are stored as secondary artists.
        var addedAt = playlistItemNode.hasNonNull("added_at")
                ? OffsetDateTime.parse(playlistItemNode.path("added_at").asText())
                : null;
        var mainArtist = trackArtists.isEmpty() ? null : trackArtists.getFirst();
        var secondaryArtists = trackArtists.size() <= 1
                ? Set.<SpotifyArtist>of()
                : trackArtists.subList(1, trackArtists.size()).stream()
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));

        return SpotifyTrack.builder()
                .spotifyTrackId(trackNode.path("id").asText())
                .sourcePlaylistId(spotifyProperties.playlistId())
                .album(album)
                .mainArtist(mainArtist)
                .secondaryArtists(secondaryArtists)
                .name(trackNode.path("name").asText(""))
                .artistNames(extractArtistNames(trackNode.path("artists")))
                .spotifyUrl(readText(trackNode.path("external_urls"), "spotify").orElse(null))
                .durationMs(trackNode.hasNonNull("duration_ms") ? trackNode.path("duration_ms").asInt() : null)
                .discNumber(trackNode.hasNonNull("disc_number") ? trackNode.path("disc_number").asInt() : null)
                .trackNumber(trackNode.hasNonNull("track_number") ? trackNode.path("track_number").asInt() : null)
                .addedToPlaylistAt(addedAt)
                .build();
    }

    // Resolves each raw artist JSON object into the shared in-memory artist map
    // so tracks that mention the same Spotify artist end up pointing to the
    // same entity instance during reconciliation.
    private List<SpotifyArtist> resolveArtists(
            Map<String, SpotifyArtist> artistsById,
            JsonNode artistsNode
    ) {
        var resolvedArtists = new ArrayList<SpotifyArtist>();
        if (artistsNode == null || !artistsNode.isArray()) {
            return resolvedArtists;
        }

        for (var artistNode : artistsNode) {
            var artist = toArtist(artistNode);
            if (artist.isEmpty()) {
                continue;
            }

            artistsById.putIfAbsent(artist.get().getSpotifyArtistId(), artist.get());
            resolvedArtists.add(artistsById.get(artist.get().getSpotifyArtistId()));
        }
        return resolvedArtists;
    }

    // Converts a simplified Spotify artist object into the normalized local
    // artist entity when the required Spotify id is present.
    private Optional<SpotifyArtist> toArtist(JsonNode artistNode) {
        return readText(artistNode, "id")
                .map(spotifyArtistId -> SpotifyArtist.builder()
                        .spotifyArtistId(spotifyArtistId)
                        .name(artistNode.path("name").asText(""))
                        .spotifyUrl(readText(artistNode.path("external_urls"), "spotify").orElse(null))
                        .href(readText(artistNode, "href").orElse(null))
                        .uri(readText(artistNode, "uri").orElse(null))
                        .type(readText(artistNode, "type").orElse(null))
                        .build());
    }

    // Keeps a denormalized comma-separated artist string on the track for quick
    // display and text use cases without having to traverse relations.
    private String extractArtistNames(JsonNode artistsNode) {
        if (artistsNode == null || !artistsNode.isArray()) {
            return "";
        }

        var artistNames = new StringBuilder();
        for (var artistNode : artistsNode) {
            var artistName = artistNode.path("name").asText("");
            if (artistName.isBlank()) {
                continue;
            }

            if (!artistNames.isEmpty()) {
                artistNames.append(", ");
            }
            artistNames.append(artistName);
        }
        return artistNames.toString();
    }

    // Uses the first album image returned by Spotify as the canonical cover URL
    // for now. That keeps the sync simple while still preserving artwork.
    private Optional<String> extractCoverImageUrl(JsonNode imagesNode) {
        if (imagesNode == null || !imagesNode.isArray() || imagesNode.isEmpty()) {
            return Optional.empty();
        }
        return readText(imagesNode.get(0), "url");
    }

    // Small null-safe text extractor used across the JSON mapping helpers.
    private Optional<String> readText(JsonNode node, String fieldName) {
        if (node == null) {
            return Optional.empty();
        }

        var value = node.path(fieldName).asText(null);
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(value);
    }

    // The sync is meaningless without the canonical playlist configured, so we
    // fail early before making any outbound Spotify call.
    private String requireConfiguredPlaylistId() {
        var playlistId = spotifyProperties.playlistId();
        if (playlistId == null || playlistId.isBlank()) {
            throw new SpotifyException(400, "Spotify playlist ID is not configured");
        }
        return playlistId;
    }
}
