package com.marcoromanofinaa.jazzlogs.core.outbox.exception;

import com.marcoromanofinaa.jazzlogs.core.exception.ApplicationException;
import org.springframework.http.HttpStatus;

public class OutboxEventPayloadDeserializationException extends ApplicationException {

    public OutboxEventPayloadDeserializationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
