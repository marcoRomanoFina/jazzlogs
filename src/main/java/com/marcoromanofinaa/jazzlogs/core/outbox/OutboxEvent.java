package com.marcoromanofinaa.jazzlogs.core.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "outbox_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private OutboxEventType type;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    private OutboxEvent(
            OutboxEventType type,
            String payload,
            OutboxEventStatus status,
            Integer retryCount,
            Instant nextRetryAt,
            Instant createdAt,
            Instant lastAttemptAt,
            Instant processedAt
    ) {
        this.type = type;
        this.payload = payload;
        this.status = status;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.createdAt = createdAt;
        this.lastAttemptAt = lastAttemptAt;
        this.processedAt = processedAt;
    }

    public static OutboxEvent pending(OutboxEventType type, String payload, Instant now) {
        return new OutboxEvent(
                type,
                payload,
                OutboxEventStatus.PENDING,
                0,
                null,
                now,
                null,
                null
        );
    }

    public void markProcessing(Instant now) {
        this.status = OutboxEventStatus.PROCESSING;
        this.lastAttemptAt = now;
    }

    public void markProcessed(Instant now) {
        this.status = OutboxEventStatus.PROCESSED;
        this.processedAt = now;
        this.nextRetryAt = null;
    }

    public void markFailedForRetry(Instant nextRetryAt, Instant now) {
        this.retryCount = this.retryCount + 1;
        this.status = OutboxEventStatus.PENDING;
        this.nextRetryAt = nextRetryAt;
        this.lastAttemptAt = now;
    }

    public void markFailed(Instant now) {
        this.retryCount = this.retryCount + 1;
        this.status = OutboxEventStatus.FAILED;
        this.lastAttemptAt = now;
        this.nextRetryAt = null;
    }
}
