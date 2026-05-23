package com.marcoromanofinaa.jazzlogs.core.outbox.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventType;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class OutboxEventProcessingException extends ApplicationException {

    public OutboxEventProcessingException(UUID eventId, OutboxEventType type, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to process outbox event " + eventId + " of type " + type);
        initCause(cause);
    }
}
