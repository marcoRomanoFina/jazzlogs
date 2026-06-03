package com.marcoromanofinaa.jazzlogs.core.outbox;

import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class OutboxCleanupJob {

    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    @Scheduled(cron = "0 */15 * * * *")
    public void cleanProcessedAndFailedEvents() {
        var deletedCount = outboxEventRepository.deleteAllByStatusIn(
                List.of(OutboxEventStatus.PROCESSED, OutboxEventStatus.FAILED)
        );

        if (deletedCount > 0) {
            log.info("Outbox cleanup deleted {} processed/failed events", deletedCount);
        }
    }
}
