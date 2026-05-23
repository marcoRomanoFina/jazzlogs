package com.marcoromanofinaa.jazzlogs.core.outbox.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import com.marcoromanofinaa.jazzlogs.core.outbox.OutboxEventType;
import org.springframework.http.HttpStatus;

public class OutboxEventHandlerNotFoundException extends ApplicationException {

    public OutboxEventHandlerNotFoundException(OutboxEventType type) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "No outbox event handler found for type: " + type);
    }
}
