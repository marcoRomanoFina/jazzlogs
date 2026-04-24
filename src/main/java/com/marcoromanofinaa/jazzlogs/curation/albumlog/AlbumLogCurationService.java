package com.marcoromanofinaa.jazzlogs.curation.albumlog;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertAlbumLogRequest;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
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
        var data = new AlbumLogData(
                request.logNumber(),
                request.album(),
                request.artist(),
                request.caption(),
                request.postedAt(),
                request.instagramPermalink(),
                request.style(),
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
        var albumLog = AlbumLog.create(data);

        albumLogRepository.findByLogNumber(request.logNumber())
                .ifPresentOrElse(existingAlbumLog -> {
                    log.info("Updating album log curation for logNumber={}", request.logNumber());
                    existingAlbumLog.update(data);
                    albumLogRepository.save(existingAlbumLog);
                }, () -> {
                    log.info("Creating album log curation for logNumber={}", request.logNumber());
                    albumLogRepository.save(albumLog);
                });
        indexingRequestPublisher.requestAlbumLogReindex(request.logNumber());
        return true;
    }
}
