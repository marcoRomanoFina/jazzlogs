package com.marcoromanofinaa.jazzlogs.core.outbox;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class OutboxEventProcessingCoordinator {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public List<OutboxEvent> claimPendingEvents(Instant now, int limit) {
        var events = outboxEventRepository.findPendingEventsForProcessing(now, limit);
        events.forEach(event -> event.markProcessing(now));
        return events;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(UUID eventId, Instant now) {
        var event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
        event.markProcessed(now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedForRetry(UUID eventId, Instant nextRetryAt, Instant now) {
        var event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
        event.markFailedForRetry(nextRetryAt, now);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID eventId, Instant now) {
        var event = outboxEventRepository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Outbox event not found: " + eventId));
        event.markFailed(now);
    }
}
