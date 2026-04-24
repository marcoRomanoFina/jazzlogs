package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "jazzlogs.ai.semantic.indexing.recovery",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class SemanticIndexingFailureRetryScheduler {

    private final SemanticIndexingRequestProcessor requestProcessor;

    // TODO: si el volumen crece, mover esta recuperación a un worker dedicado en vez de scheduler in-process.
    @Scheduled(
            cron = "${jazzlogs.ai.semantic.indexing.recovery.cron:0 */10 * * * *}",
            zone = "${jazzlogs.ai.semantic.indexing.recovery.zone:America/Argentina/Buenos_Aires}"
    )
    public void retryFailures() {
        try {
            requestProcessor.retryPersistedFailures();
        } catch (Exception exception) {
            log.error("Scheduled semantic indexing failure retry failed", exception);
        }
    }
}
