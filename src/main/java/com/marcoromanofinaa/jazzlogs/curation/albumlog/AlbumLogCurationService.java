package com.marcoromanofinaa.jazzlogs.curation.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertAlbumLogRequest;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlbumLogCurationService {

    private final AlbumLogRepository albumLogRepository;
    private final SpotifyAlbumRepository spotifyAlbumRepository;
    private final SemanticIndexingRequestPublisher indexingRequestPublisher;

    @Transactional
    public boolean upsert(UpsertAlbumLogRequest request) {
        var data = new AlbumLogData(
                request.logNumber(),
                request.album(),
                request.mainArtists(),
                request.caption(),
                request.postedAt(),
                request.instagramPermalink(),
                request.style(),
                request.vocalProfile(),
                request.releaseYear(),
                request.moods().toArray(String[]::new),
                request.tier(),
                request.vibe().toArray(String[]::new),
                request.energy(),
                request.moodIntensity(),
                request.accessibility(),
                request.bestMoment(),
                request.listeningContext().toArray(String[]::new),
                request.notes(),
                request.whyItMatters(),
                request.editorialNote(),
                request.recommendedIf(),
                request.avoidIf(),
                request.albumContext(),
                request.personnel(),
                request.spotifyAlbumId()
        );
        var spotifyAlbum = resolveSpotifyAlbum(request.spotifyAlbumId());
        var albumLog = AlbumLog.create(data);
        albumLog.linkSpotifyAlbum(spotifyAlbum);

        albumLogRepository.findByLogNumber(request.logNumber())
                .ifPresentOrElse(existingAlbumLog -> {
                    log.info("Updating album log curation for logNumber={}", request.logNumber());
                    existingAlbumLog.update(data);
                    existingAlbumLog.linkSpotifyAlbum(spotifyAlbum);
                    albumLogRepository.save(existingAlbumLog);
                }, () -> {
                    log.info("Creating album log curation for logNumber={}", request.logNumber());
                    albumLogRepository.save(albumLog);
                });
        indexingRequestPublisher.requestAlbumLogReindex(request.logNumber());
        return true;
    }

    private com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum resolveSpotifyAlbum(String spotifyAlbumId) {
        if (spotifyAlbumId == null || spotifyAlbumId.isBlank()) {
            return null;
        }

        return spotifyAlbumRepository.findById(spotifyAlbumId)
                .orElseThrow(() -> new SpotifyException(400, "Spotify album not found for id " + spotifyAlbumId));
    }
}
