package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestProcessor;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRetryProperties;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SemanticIndexingTransactionalListenerTest {

    @Test
    void delegatesRequestWhenProcessorIsAvailable() {
        var processor = new CapturingSemanticIndexingRequestProcessor();
        var listener = new SemanticIndexingTransactionalListener(Optional.of(processor));
        var request = new SemanticIndexingRequest(SemanticDocumentType.ALBUM_LOG, "77");

        listener.onSemanticIndexingRequested(request);

        assertThat(processor.capturedRequest).isEqualTo(request);
    }

    @Test
    void skipsRequestWhenProcessorIsUnavailable() {
        var listener = new SemanticIndexingTransactionalListener(Optional.empty());
        var request = new SemanticIndexingRequest(SemanticDocumentType.ALBUM_LOG, "77");

        listener.onSemanticIndexingRequested(request);
    }

    private static class CapturingSemanticIndexingRequestProcessor extends SemanticIndexingRequestProcessor {

        private SemanticIndexingRequest capturedRequest;

        private CapturingSemanticIndexingRequestProcessor() {
            super(null, null, new SemanticIndexingRetryProperties());
        }

        @Override
        public void process(SemanticIndexingRequest request) {
            this.capturedRequest = request;
        }
    }
}
