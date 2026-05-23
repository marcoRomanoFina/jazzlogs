package com.marcoromanofinaa.jazzlogs.core.outbox;

public interface OutboxEventHandler {

    boolean supports(OutboxEventType type);

    void handle(OutboxEvent event);
}
