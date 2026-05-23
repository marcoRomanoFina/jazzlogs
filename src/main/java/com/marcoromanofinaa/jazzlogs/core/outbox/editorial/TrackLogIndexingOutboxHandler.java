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
public class TrackLogIndexingOutboxHandler implements OutboxEventHandler {

    private final EditorialIndexingService editorialIndexingService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(OutboxEventType type) {
        return type == OutboxEventType.TRACK_LOG_INDEXING_REQUESTED;
    }

    @Override
    public void handle(OutboxEvent event) {
        try {
            TrackLogPayload payload = objectMapper.readValue(
                    event.getPayload(),
                    TrackLogPayload.class
            );

            editorialIndexingService.indexTrackLog(payload.trackLogId());
        } catch (JsonProcessingException exception) {
            throw new OutboxEventPayloadDeserializationException(
                    "Failed to deserialize TrackLog outbox payload",
                    exception
            );
        }
    }
}
