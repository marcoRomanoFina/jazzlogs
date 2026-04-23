package com.marcoromanofinaa.jazzlogs.curation.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertAlbumLogRequest;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AlbumLogCurationService {

    private final AlbumLogRepository albumLogRepository;
    private final SemanticIndexingRequestPublisher indexingRequestPublisher;

    @Transactional
    public boolean upsert(UpsertAlbumLogRequest request) {
        var moods = request.moods().toArray(String[]::new);
        var vibe = request.vibe().toArray(String[]::new);
        var listeningContext = request.listeningContext().toArray(String[]::new);
        var albumLog = AlbumLog.create(
                request.logNumber(),
                request.album(),
                request.artist(),
                request.caption(),
                request.postedAt(),
                request.instagramPermalink(),
                request.style(),
                request.releaseYear(),
                moods,
                request.tier(),
                vibe,
                request.energy(),
                request.moodIntensity(),
                request.accessibility(),
                request.bestMoment(),
                listeningContext,
                request.notes(),
                request.whyItMatters(),
                request.editorialNote(),
                request.recommendedIf(),
                request.avoidIf(),
                request.albumContext(),
                request.personnel(),
                request.spotifyAlbumId()
        );

        albumLogRepository.findByLogNumber(request.logNumber())
                .ifPresentOrElse(existingAlbumLog -> {
                    log.info("Updating album log curation for logNumber={}", request.logNumber());
                    existingAlbumLog.update(albumLog);
                    albumLogRepository.save(existingAlbumLog);
                }, () -> {
                    log.info("Creating album log curation for logNumber={}", request.logNumber());
                    albumLogRepository.save(albumLog);
                });
        indexingRequestPublisher.requestAlbumLogReindex(request.logNumber());
        return true;
    }
}
