package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequest;
import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.indexer.SemanticDocumentIndexer;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import com.marcoromanofinaa.jazzlogs.core.exception.VectorStoreNotConfiguredException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class SemanticDocumentIndexingService {

    public static final String TRANSFORMER_VERSION = "semantic-template-v1";

    private final List<SemanticDocumentIndexer> indexers;
    private final SemanticDocumentVectorStoreMapper vectorStoreMapper;
    private final Optional<VectorStore> vectorStore;

    public SemanticDocumentIndexingResult indexAll() {
        log.info("Starting full semantic document rebuild");
        var documents = allDocuments();
        documents.forEach(this::upsertDocumentInVectorStore);
        var result = new SemanticDocumentIndexingResult(documents.size(), documents.size());
        log.info("Finished full semantic document rebuild requested={} indexed={}", result.requested(), result.indexed());
        return result;
    }

    public void indexOne(SemanticIndexingRequest request) {
        upsertDocumentInVectorStore(indexerFor(request).indexOne(request.sourceIdentifier()));
    }

    public boolean isConfigured() {
        return vectorStore.isPresent();
    }

    private List<SemanticDocument> allDocuments() {
        return indexers.stream()
                .flatMap(indexer -> indexer.indexAll().stream())
                .toList();
    }

    private SemanticDocumentIndexer indexerFor(SemanticIndexingRequest request) {
        return indexers.stream()
                .filter(indexer -> indexer.type() == request.type())
                .findFirst()
                .orElseThrow(() -> new FeatureUnavailableException("Unsupported semantic indexing type: " + request.type()));
    }

    private void upsertDocumentInVectorStore(SemanticDocument semanticDocument) {
        var document = vectorStoreMapper.toSpringAiDocument(semanticDocument, TRANSFORMER_VERSION);
        // TODO: revisar si el vector store concreto soporta upsert real para evitar la ventana delete+add.
        requiredVectorStore().delete(List.of(document.getId()));
        requiredVectorStore().add(List.of(document));
    }

    private VectorStore requiredVectorStore() {
        return vectorStore.orElseThrow(VectorStoreNotConfiguredException::new);
    }
}
