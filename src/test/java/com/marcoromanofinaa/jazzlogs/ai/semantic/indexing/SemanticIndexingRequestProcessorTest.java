package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import static org.assertj.core.api.Assertions.assertThat;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequest;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure.SemanticIndexingFailure;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.failure.SemanticIndexingFailureRepository;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SemanticIndexingRequestProcessorTest {

    @Test
    void clearsFailureWhenIndexingSucceeds() {
        var request = new SemanticIndexingRequest(SemanticDocumentType.TRACK_NOTE, "track-1");
        var indexingService = new FlakySemanticDocumentIndexingService(false);
        var recorder = new FailureRepositoryRecorder();
        var processor = new SemanticIndexingRequestProcessor(
                indexingService,
                recorder.repository(),
                retryProperties(25)
        );

        processor.process(request);

        assertThat(indexingService.attempts).isEqualTo(1);
        assertThat(recorder.deletedKeys).containsExactly(keyOf(request));
        assertThat(recorder.savedFailures).isEmpty();
    }

    @Test
    void persistsFailureWhenIndexingFails() {
        var request = new SemanticIndexingRequest(SemanticDocumentType.ARTIST_PROFILE, "artist-1");
        var indexingService = new FlakySemanticDocumentIndexingService(true);
        var recorder = new FailureRepositoryRecorder();
        recorder.existingFailure = Optional.of(SemanticIndexingFailure.create(request.type(), request.sourceIdentifier()));
        var processor = new SemanticIndexingRequestProcessor(
                indexingService,
                recorder.repository(),
                retryProperties(25)
        );

        try {
            processor.process(request);
        } catch (RuntimeException exception) {
            assertThat(exception).hasMessage("temporary failure");
        }

        assertThat(indexingService.attempts).isEqualTo(1);
        assertThat(recorder.savedFailures).hasSize(1);
        assertThat(recorder.deletedKeys).isEmpty();
        assertThat(recorder.savedFailures.getFirst().getFailedAttempts()).isEqualTo(1);
    }

    private SemanticIndexingRetryProperties retryProperties(int recoveryBatchSize) {
        var properties = new SemanticIndexingRetryProperties();
        properties.setRecoveryBatchSize(recoveryBatchSize);
        return properties;
    }

    private String keyOf(SemanticIndexingRequest request) {
        return request.type() + ":" + request.sourceIdentifier();
    }

    private static class FlakySemanticDocumentIndexingService extends SemanticDocumentIndexingService {

        private final boolean shouldFail;
        private int attempts;

        private FlakySemanticDocumentIndexingService(boolean shouldFail) {
            super(List.of(), null, null);
            this.shouldFail = shouldFail;
        }

        @Override
        public void indexOne(SemanticIndexingRequest request) {
            attempts++;
            if (shouldFail) {
                throw new RuntimeException("temporary failure");
            }
        }
    }

    private static class FailureRepositoryRecorder {

        private final List<String> deletedKeys = new ArrayList<>();
        private final List<SemanticIndexingFailure> savedFailures = new ArrayList<>();
        private Optional<SemanticIndexingFailure> existingFailure = Optional.empty();

        private SemanticIndexingFailureRepository repository() {
            return (SemanticIndexingFailureRepository) Proxy.newProxyInstance(
                    SemanticIndexingFailureRepository.class.getClassLoader(),
                    new Class[]{SemanticIndexingFailureRepository.class},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "findByTypeAndSourceIdentifier" -> existingFailure;
                        case "deleteByTypeAndSourceIdentifier" -> {
                            deletedKeys.add(args[0] + ":" + args[1]);
                            yield null;
                        }
                        case "save" -> {
                            savedFailures.add((SemanticIndexingFailure) args[0]);
                            yield args[0];
                        }
                        case "toString" -> "FailureRepositoryRecorder";
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "equals" -> proxy == args[0];
                        default -> throw new UnsupportedOperationException(method.getName());
                    }
            );
        }
    }
}
