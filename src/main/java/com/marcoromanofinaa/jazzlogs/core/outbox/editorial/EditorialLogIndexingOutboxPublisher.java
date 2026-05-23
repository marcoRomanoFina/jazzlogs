package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.model.ArtistLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEvent;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventRepository;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventType;
import com.marcoromanofinaa.jazzlogs.core.outbox.exception.OutboxEventPayloadSerializationException;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EditorialLogIndexingOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void publishAlbumLogIndexingRequested(AlbumLog albumLog, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new AlbumLogPayload(albumLog.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.ALBUM_LOG_INDEXING_REQUESTED, payload, requestedAt);
    }

    public void publishTrackLogIndexingRequested(TrackLog trackLog, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new TrackLogPayload(trackLog.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.TRACK_LOG_INDEXING_REQUESTED, payload, requestedAt);
    }

    public void publishArtistLogIndexingRequested(ArtistLog artistLog, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new ArtistLogPayload(artistLog.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.ARTIST_LOG_INDEXING_REQUESTED, payload, requestedAt);
    }

    private void savePendingEvent(OutboxEventType type, Object payload, Instant now) {
        outboxEventRepository.save(OutboxEvent.pending(type, serialize(payload), now));
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new OutboxEventPayloadSerializationException("Failed to serialize outbox payload", exception);
        }
    }
}
