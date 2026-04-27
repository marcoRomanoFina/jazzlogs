package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@Slf4j
@RequiredArgsConstructor
public class SemanticIndexingTransactionalListener {

    private final SemanticIndexingRequestProcessor requestProcessor;

    /*
     * El vector store es una proyección externa. Por eso la escritura en la DB primero commitea
     * y recién después intentamos sincronizar embeddings. Si falla, la verdad sigue siendo la DB.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSemanticIndexingRequested(SemanticIndexingRequest request) {
        if (!requestProcessor.isConfigured()) {
            log.info(
                    "Skipping semantic reindex for type={} sourceIdentifier={} because vector store is not configured",
                    request.type(),
                    request.sourceIdentifier()
            );
            return;
        }

        log.info(
                "Processing semantic reindex request after commit for type={} sourceIdentifier={}",
                request.type(),
                request.sourceIdentifier()
        );
        requestProcessor.process(request);
    }
}
