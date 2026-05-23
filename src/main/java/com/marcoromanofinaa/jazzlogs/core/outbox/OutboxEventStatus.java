package com.marcoromanofinaa.jazzlogs.core.outbox;

public enum OutboxEventStatus {
    PENDING,
    PROCESSING,
    PROCESSED,
    FAILED
}
