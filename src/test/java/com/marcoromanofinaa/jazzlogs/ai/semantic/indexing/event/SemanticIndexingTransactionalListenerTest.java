package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestProcessor;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRetryProperties;
import org.junit.jupiter.api.Test;

class SemanticIndexingTransactionalListenerTest {

    @Test
    void delegatesRequestWhenProcessorIsAvailable() {
        var processor = new CapturingSemanticIndexingRequestProcessor();
        var listener = new SemanticIndexingTransactionalListener(processor);
        var request = new SemanticIndexingRequest(SemanticDocumentType.ALBUM_LOG, "77");

        listener.onSemanticIndexingRequested(request);

        assertThat(processor.capturedRequest).isEqualTo(request);
    }

    @Test
    void skipsRequestWhenProcessorIsUnavailable() {
        var listener = new SemanticIndexingTransactionalListener(new CapturingSemanticIndexingRequestProcessor(false));
        var request = new SemanticIndexingRequest(SemanticDocumentType.ALBUM_LOG, "77");

        listener.onSemanticIndexingRequested(request);
    }

    private static class CapturingSemanticIndexingRequestProcessor extends SemanticIndexingRequestProcessor {

        private final boolean configured;
        private SemanticIndexingRequest capturedRequest;

        private CapturingSemanticIndexingRequestProcessor() {
            this(true);
        }

        private CapturingSemanticIndexingRequestProcessor(boolean configured) {
            super(null, null, new SemanticIndexingRetryProperties());
            this.configured = configured;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public void process(SemanticIndexingRequest request) {
            this.capturedRequest = request;
        }
    }
}
