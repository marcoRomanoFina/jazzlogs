package com.marcoromanofinaa.jazzlogs.spotify.playlist;

import com.fasterxml.jackson.databind.JsonNode;
import com.marcoromanofinaa.jazzlogs.spotify.auth.SpotifyConnectionService;
import com.marcoromanofinaa.jazzlogs.spotify.binding.SpotifyAlbumLogBindingService;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtistRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtistSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackSyncData;
import com.marcoromanofinaa.jazzlogs.spotify.client.SpotifyApiClient;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyProperties;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
// Sincroniza la playlist canónica de Spotify en tablas locales normalizadas. El
// sync funciona como una reconciliación delta: trae el snapshot actual de Spotify,
// lo compara contra el snapshot de la base y aplica sólo inserts, updates y
// removals en vez de borrar todo en cada corrida.
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
    // Punto de entrada principal del sync: pagina la playlist, junta álbumes,
    // artistas y tracks en memoria, los reconcilia contra filas existentes,
    // persiste el delta y luego vincula logs editoriales no enlazados con los
    // álbumes sincronizados de Spotify.
    public int syncConfiguredPlaylist() {
        var playlistId = requireConfiguredPlaylistId();
        log.info("Starting Spotify playlist sync for playlistId={}", playlistId);

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

        log.info(
                "Fetched Spotify playlist snapshot for playlistId={} with {} tracks, {} albums and {} artists",
                playlistId,
                tracksById.size(),
                albumsById.size(),
                artistsById.size()
        );

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
        var boundLogs = spotifyAlbumLogBindingService.bindConfiguredPlaylistAlbumsToLogs();
        log.info(
                "Completed Spotify playlist sync for playlistId={} with {} tracks persisted and {} album logs bound",
                playlistId,
                tracksById.size(),
                boundLogs
        );
        return tracksById.size();
    }

    // Reutiliza filas de álbum existentes cuando se puede y sólo actualiza los
    // campos que consideramos parte del snapshot sincronizado de Spotify.
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

    // Los artistas son entidades globales de Spotify, así que la reconciliación
    // se hace por spotify artist id y no por pertenencia a una playlist.
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

    // Los tracks son el objeto más rico del sync porque hay que volver a
    // enlazarlos con las instancias reconciliadas de álbum y artista antes de guardarlos.
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

    // Borra tracks que antes pertenecían al snapshot de la playlist pero que
    // Spotify ya no devuelve.
    private void deleteRemovedTracks(
            Map<String, SpotifyTrack> existingTracksById,
            Map<String, SpotifyTrack> syncedTracksById
    ) {
        var removedTracks = existingTracksById.entrySet().stream()
                .filter(entry -> !syncedTracksById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedTracks.isEmpty()) {
            log.info("Removing {} Spotify tracks no longer present in playlist snapshot", removedTracks.size());
            spotifyTrackRepository.deleteAll(removedTracks);
            spotifyTrackRepository.flush();
        }
    }

    // Borra álbumes de la playlist que desaparecieron del último snapshot de Spotify.
    private void deleteRemovedAlbums(
            Map<String, SpotifyAlbum> existingAlbumsById,
            Map<String, SpotifyAlbum> syncedAlbumsById
    ) {
        var removedAlbums = existingAlbumsById.entrySet().stream()
                .filter(entry -> !syncedAlbumsById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedAlbums.isEmpty()) {
            log.info("Removing {} Spotify albums no longer present in playlist snapshot", removedAlbums.size());
            spotifyAlbumRepository.deleteAll(removedAlbums);
            spotifyAlbumRepository.flush();
        }
    }

    // Borra artistas que ya no están referenciados por el snapshot sincronizado
    // de la playlist después de reconciliar tracks y álbumes.
    private void deleteRemovedArtists(
            Map<String, SpotifyArtist> existingArtistsById,
            Map<String, SpotifyArtist> syncedArtistsById
    ) {
        var removedArtists = existingArtistsById.entrySet().stream()
                .filter(entry -> !syncedArtistsById.containsKey(entry.getKey()))
                .map(Map.Entry::getValue)
                .toList();

        if (!removedArtists.isEmpty()) {
            log.info("Removing {} Spotify artists no longer referenced by playlist snapshot", removedArtists.size());
            spotifyArtistRepository.deleteAll(removedArtists);
            spotifyArtistRepository.flush();
        }
    }

    // Recorre la página JSON cruda de la playlist y arma entidades en memoria
    // indexadas por Spotify ids. Eso le da a la reconciliación un snapshot local
    // limpio sin persistir filas parciales página por página.
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

    // Spotify puede devolver el payload del item de playlist bajo `item` en la
    // proyección de fields que pedimos, pero este fallback mantiene tolerante el parser.
    private JsonNode resolveTrackNode(JsonNode playlistItemNode) {
        if (playlistItemNode.hasNonNull("item")) {
            return playlistItemNode.path("item");
        }
        return playlistItemNode.path("track");
    }

    // Convierte el objeto JSON anidado del álbum de Spotify en la entidad de
    // álbum normalizada que usa el catálogo local.
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
        // Spotify devuelve artistas ordenados, así que el primero queda como
        // artista principal y el resto se guarda como artistas secundarios.
        var addedAt = playlistItemNode.hasNonNull("added_at")
                ? Instant.parse(playlistItemNode.path("added_at").asText())
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
                .spotifyUrl(readText(trackNode.path("external_urls"), "spotify").orElse(null))
                .durationMs(trackNode.hasNonNull("duration_ms") ? trackNode.path("duration_ms").asInt() : null)
                .discNumber(trackNode.hasNonNull("disc_number") ? trackNode.path("disc_number").asInt() : null)
                .trackNumber(trackNode.hasNonNull("track_number") ? trackNode.path("track_number").asInt() : null)
                .addedToPlaylistAt(addedAt)
                .build();
    }

    // Resuelve cada objeto JSON crudo de artista dentro del mapa compartido en
    // memoria para que tracks que mencionan el mismo artista de Spotify terminen
    // apuntando a la misma instancia durante la reconciliación.
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

    // Convierte un objeto simplificado de artista de Spotify en la entidad local
    // normalizada cuando está presente el Spotify id requerido.
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

    // Usa la primera imagen del álbum devuelta por Spotify como cover URL
    // canónica por ahora. Eso mantiene simple el sync sin perder artwork.
    private Optional<String> extractCoverImageUrl(JsonNode imagesNode) {
        if (imagesNode == null || !imagesNode.isArray() || imagesNode.isEmpty()) {
            return Optional.empty();
        }
        return readText(imagesNode.get(0), "url");
    }

    // Extractor chico de texto null-safe usado por los helpers de mapeo JSON.
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

    // El sync no tiene sentido sin la playlist canónica configurada, así que
    // fallamos temprano antes de hacer cualquier llamada saliente a Spotify.
    private String requireConfiguredPlaylistId() {
        var playlistId = spotifyProperties.playlistId();
        if (playlistId == null || playlistId.isBlank()) {
            throw new SpotifyException(400, "Spotify playlist ID is not configured");
        }
        return playlistId;
    }
}
