package com.marcoromanofinaa.jazzlogs.core.outbox;

import com.marcoromanofinaa.jazzlogs.core.outbox.exception.OutboxEventProcessingException;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxEventProcessorJob {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxEventHandlerResolver outboxEventHandlerResolver;
    private final OutboxProperties outboxProperties;
    private final Clock clock;

    @Scheduled(
            cron = "${jazzlogs.outbox.processing.cron:0 */1 * * * *}",
            zone = "${jazzlogs.outbox.processing.zone:America/Argentina/Buenos_Aires}"
    )
    @Transactional
    public void processPendingEvents() {
        var now = Instant.now(clock);
        var processing = outboxProperties.processing();
        var events = outboxEventRepository.findPendingEventsForProcessing(now, processing.batchSize());

        for (var event : events) {
            processSingleEvent(event, now, processing);
        }
    }

    private void processSingleEvent(
            OutboxEvent event,
            Instant now,
            OutboxProperties.Processing processing
    ) {
        event.markProcessing(now);

        try {
            var handler = outboxEventHandlerResolver.resolve(event.getType());
            handler.handle(event);
            event.markProcessed(Instant.now(clock));
        } catch (Exception exception) {
            var processingException = new OutboxEventProcessingException(event.getId(), event.getType(), exception);
            if (event.getRetryCount() + 1 >= processing.maxRetries()) {
                event.markFailed(Instant.now(clock));
                log.error("Outbox event {} failed permanently after {} attempts", event.getId(), event.getRetryCount() + 1, processingException);
                return;
            }

            var nextRetryAt = calculateNextRetryAt(event, Instant.now(clock), processing);
            event.markFailedForRetry(nextRetryAt, Instant.now(clock));
            log.warn("Outbox event {} failed, scheduled retry {} at {}", event.getId(), event.getRetryCount(), nextRetryAt, processingException);
        }
    }

    private Instant calculateNextRetryAt(
            OutboxEvent event,
            Instant now,
            OutboxProperties.Processing processing
    ) {
        long multiplier = 1L;
        int attemptsAfterFailure = event.getRetryCount() + 1;
        for (int i = 1; i < attemptsAfterFailure; i++) {
            multiplier *= Math.max(1, processing.retryBackoffMultiplier());
        }
        return now.plus(processing.retryDelay().multipliedBy(multiplier));
    }
}
