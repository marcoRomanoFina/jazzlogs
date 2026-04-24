package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SemanticIndexingRequestPublisher {

    private final ApplicationEventPublisher eventPublisher;

    public void requestAlbumLogReindex(Integer logNumber) {
        publish(SemanticDocumentType.ALBUM_LOG, String.valueOf(logNumber));
    }

    public void requestTrackNoteReindex(String spotifyTrackId) {
        publish(SemanticDocumentType.TRACK_NOTE, spotifyTrackId);
    }

    public void requestArtistProfileReindex(String spotifyArtistId) {
        publish(SemanticDocumentType.ARTIST_PROFILE, spotifyArtistId);
    }

    private void publish(SemanticDocumentType type, String sourceIdentifier) {
        eventPublisher.publishEvent(new SemanticIndexingRequest(type, sourceIdentifier));
    }
}
