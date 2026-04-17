package com.marcoromanofinaa.jazzlogs.spotify.application;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyAlbumRepository;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// Binds editorial album logs to Spotify albums after the playlist sync. This is
// intentionally deterministic now: if a log includes a Spotify album seed id,
// we bind it directly; otherwise we leave the log untouched.
public class SpotifyAlbumLogBindingService {

    private final SpotifyProperties spotifyProperties;
    private final AlbumLogRepository albumLogRepository;
    private final SpotifyAlbumRepository spotifyAlbumRepository;

    @Transactional
    // Only unresolved logs are considered here, so previously bound logs keep
    // their existing association unless changed elsewhere explicitly.
    public int bindConfiguredPlaylistAlbumsToLogs() {
        var playlistId = requireConfiguredPlaylistId();
        var albums = spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc(playlistId);
        var albumsById = indexAlbumsById(albums);
        var logs = albumLogRepository.findAllBySpotifyAlbumIsNullOrderByLogNumberAsc();
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

        return matchedLogs.size();
    }

    // Binding is currently a direct lookup by the editorially curated Spotify
    // album id stored in the JSON seed and persisted on AlbumLog.
    private Optional<SpotifyAlbum> findAlbumToBind(
            AlbumLog log,
            Map<String, SpotifyAlbum> albumsById
    ) {
        return findMatchBySeedId(log, albumsById);
    }

    // If the log carries a non-blank Spotify album seed id, try to resolve it
    // against the albums already synced from the canonical playlist.
    private Optional<SpotifyAlbum> findMatchBySeedId(AlbumLog log, Map<String, SpotifyAlbum> albumsById) {
        return Optional.ofNullable(log.getSpotifyAlbumSeedId())
                .filter(value -> !value.isBlank())
                .map(albumsById::get);
    }

    // Pre-indexes synced albums so binding can happen entirely in memory
    // without issuing one query per log.
    private Map<String, SpotifyAlbum> indexAlbumsById(List<SpotifyAlbum> albums) {
        var albumsById = new LinkedHashMap<String, SpotifyAlbum>();
        for (var album : albums) {
            albumsById.put(album.getSpotifyAlbumId(), album);
        }
        return albumsById;
    }

    // The whole Spotify import pipeline assumes one configured canonical
    // playlist, so binding stops early when that root configuration is missing.
    private String requireConfiguredPlaylistId() {
        var playlistId = spotifyProperties.playlistId();
        if (playlistId == null || playlistId.isBlank()) {
            throw new SpotifyException(400, "Spotify playlist ID is not configured");
        }
        return playlistId;
    }
}
