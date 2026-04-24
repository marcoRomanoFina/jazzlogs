package com.marcoromanofinaa.jazzlogs.spotify.binding;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyProperties;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
// Vincula logs editoriales de álbum con álbumes de Spotify después del sync de
// la playlist. Ahora es deliberadamente determinístico: si un log trae un
// Spotify album seed id, se vincula directo; si no, se deja intacto.
public class SpotifyAlbumLogBindingService {

    private final SpotifyProperties spotifyProperties;
    private final AlbumLogRepository albumLogRepository;
    private final SpotifyAlbumRepository spotifyAlbumRepository;

    @Transactional
    // Acá sólo se consideran logs no resueltos, así que los ya vinculados
    // mantienen su asociación salvo que otra parte la cambie explícitamente.
    public int bindConfiguredPlaylistAlbumsToLogs() {
        var playlistId = requireConfiguredPlaylistId();
        var albums = spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc(playlistId);
        var albumsById = indexAlbumsById(albums);
        var logs = albumLogRepository.findAllBySpotifyAlbumIsNullOrderByLogNumberAsc();
        log.info(
                "Starting Spotify album log binding for playlistId={} with {} candidate albums and {} unresolved logs",
                playlistId,
                albums.size(),
                logs.size()
        );
        var matchedLogs = new LinkedHashSet<AlbumLog>();

        for (var log : logs) {
            findAlbumToBind(log, albumsById)
                    .ifPresent(album -> {
                        log.linkSpotifyAlbum(album);
                        matchedLogs.add(log);
                    });
        }

        if (!matchedLogs.isEmpty()) {
            albumLogRepository.saveAll(matchedLogs);
        }

        log.info("Finished Spotify album log binding for playlistId={} with {} bound logs", playlistId, matchedLogs.size());
        return matchedLogs.size();
    }

    // El binding hoy es una búsqueda directa por el Spotify album id curado
    // editorialmente, persistido en AlbumLog.
    private Optional<SpotifyAlbum> findAlbumToBind(
            AlbumLog log,
            Map<String, SpotifyAlbum> albumsById
    ) {
        return findMatchBySeedId(log, albumsById);
    }

    // Si el log trae un Spotify album seed id no vacío, intentamos resolverlo
    // contra los álbumes ya sincronizados desde la playlist canónica.
    private Optional<SpotifyAlbum> findMatchBySeedId(AlbumLog log, Map<String, SpotifyAlbum> albumsById) {
        return Optional.ofNullable(log.getSpotifyAlbumSeedId())
                .filter(value -> !value.isBlank())
                .map(albumsById::get);
    }

    // Preindexa álbumes sincronizados para que el binding ocurra enteramente en
    // memoria sin disparar una query por cada log.
    private Map<String, SpotifyAlbum> indexAlbumsById(List<SpotifyAlbum> albums) {
        var albumsById = new LinkedHashMap<String, SpotifyAlbum>();
        for (var album : albums) {
            albumsById.put(album.getSpotifyAlbumId(), album);
        }
        return albumsById;
    }

    // Todo el pipeline de importación de Spotify asume una única playlist
    // canónica configurada, así que el binding corta temprano si falta esa raíz.
    private String requireConfiguredPlaylistId() {
        var playlistId = spotifyProperties.playlistId();
        if (playlistId == null || playlistId.isBlank()) {
            throw new SpotifyException(400, "Spotify playlist ID is not configured");
        }
        return playlistId;
    }
}
