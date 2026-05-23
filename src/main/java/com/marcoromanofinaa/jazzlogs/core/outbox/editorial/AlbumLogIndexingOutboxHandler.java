package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.admin.editorial.indexing.EditorialIndexingService;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEvent;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventHandler;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventType;
import com.marcoromanofinaa.jazzlogs.core.outbox.exception.OutboxEventPayloadDeserializationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlbumLogIndexingOutboxHandler implements OutboxEventHandler {

    private final EditorialIndexingService editorialIndexingService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxEventType type) {
        return type == OutboxEventType.ALBUM_LOG_INDEXING_REQUESTED;
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            AlbumLogPayload payload = objectMapper.readValue(
                    event.getPayload(),
                    AlbumLogPayload.class
            );

            editorialIndexingService.indexAlbumLog(payload.albumLogId());
        } catch (JsonProcessingException exception) {
            throw new OutboxEventPayloadDeserializationException(
                    "Failed to deserialize AlbumLog outbox payload",
                    exception
            );
        }
    }
}
