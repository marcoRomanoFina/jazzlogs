package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
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
public class EditorialEmbeddingOutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public void publishAlbumEmbeddingSyncRequested(AlbumNode albumNode, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new AlbumEditorialEmbeddingPayload(albumNode.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.ALBUM_EDITORIAL_EMBEDDING_SYNC_REQUESTED, payload, requestedAt);
    }

    public void publishTrackEmbeddingSyncRequested(TrackNode trackNode, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new TrackEditorialEmbeddingPayload(trackNode.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.TRACK_EDITORIAL_EMBEDDING_SYNC_REQUESTED, payload, requestedAt);
    }

    public void publishArtistEmbeddingSyncRequested(ArtistNode artistNode, UUID requestedByUserId) {
        var requestedAt = Instant.now(clock);
        var payload = new ArtistEditorialEmbeddingPayload(artistNode.getId(), requestedByUserId, requestedAt);
        savePendingEvent(OutboxEventType.ARTIST_EDITORIAL_EMBEDDING_SYNC_REQUESTED, payload, requestedAt);
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
