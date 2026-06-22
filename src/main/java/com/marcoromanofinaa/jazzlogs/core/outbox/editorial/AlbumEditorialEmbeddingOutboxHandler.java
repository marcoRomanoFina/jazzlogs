package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.admin.editorial.embedding.EditorialEmbeddingSyncService;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEvent;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventHandler;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventType;
import com.marcoromanofinaa.jazzlogs.core.outbox.exception.OutboxEventPayloadDeserializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumEditorialEmbeddingOutboxHandler implements OutboxEventHandler {

    private final EditorialEmbeddingSyncService editorialEmbeddingSyncService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxEventType type) {
        return type == OutboxEventType.ALBUM_EDITORIAL_EMBEDDING_SYNC_REQUESTED;
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            AlbumEditorialEmbeddingPayload payload = objectMapper.readValue(
                    event.getPayload(),
                    AlbumEditorialEmbeddingPayload.class
            );

            editorialEmbeddingSyncService.syncAlbumEmbedding(payload.albumNodeId());
        } catch (JsonProcessingException exception) {
            throw new OutboxEventPayloadDeserializationException(
                    "Failed to deserialize album editorial embedding payload",
                    exception
            );
        }
    }
}
