package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequest;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure.SemanticIndexingFailure;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure.SemanticIndexingFailureRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticIndexingRequestProcessor {

    private final SemanticDocumentIndexingService indexingService;
    private final SemanticIndexingFailureRepository failureRepository;
    private final SemanticIndexingRetryProperties retryProperties;

    public boolean isConfigured() {
        return indexingService.isConfigured();
    }

    /*
     * TODO: exponer un endpoint admin para inspeccionar failures abiertos y disparar retry manual
     * de un documento puntual cuando haga falta debugging operativo.
     */
    public void process(SemanticIndexingRequest request) {
        try {
            indexingService.indexOne(request);
            failureRepository.deleteByTypeAndSourceIdentifier(request.type(), request.sourceIdentifier());
        } catch (RuntimeException exception) {
            log.error(
                    "Semantic indexing failed for type={} sourceIdentifier={}. Persisting failure for scheduled recovery",
                    request.type(),
                    request.sourceIdentifier(),
                    exception
            );
            persistFailure(request, exception);
            throw exception;
        }
    }

    public void retryPersistedFailures() {
        if (!isConfigured()) {
            log.debug("Skipping persisted semantic failure retry because vector store is not configured");
            return;
        }

        var failures = failureRepository.findAllByOrderByLastFailedAtAsc(PageRequest.of(
                0,
                Math.max(1, retryProperties.getRecoveryBatchSize())
        ));

        if (failures.isEmpty()) {
            return;
        }

        log.info("Retrying {} persisted semantic indexing failures", failures.size());
        failures.forEach(this::retryPersistedFailure);
    }

    private void persistFailure(SemanticIndexingRequest request, RuntimeException exception) {
        var failedAt = Instant.now();
        var failure = failureRepository.findByTypeAndSourceIdentifier(request.type(), request.sourceIdentifier())
                .orElseGet(() -> SemanticIndexingFailure.create(request.type(), request.sourceIdentifier()));
        failure.recordFailure(
                exception.getClass().getName(),
                exception.getMessage(),
                failure.getFailedAttempts() + 1,
                failedAt
        );
        // Guardamos un solo registro por documento para que la tabla sea una cola chica de recuperación.
        failureRepository.save(failure);
    }

    private void retryPersistedFailure(SemanticIndexingFailure failure) {
        var request = new SemanticIndexingRequest(failure.getType(), failure.getSourceIdentifier());

        try {
            process(request);
        } catch (RuntimeException exception) {
            log.warn(
                    "Scheduled semantic failure retry still failing for type={} sourceIdentifier={}",
                    failure.getType(),
                    failure.getSourceIdentifier(),
                    exception
            );
        }
    }
}
