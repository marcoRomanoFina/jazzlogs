package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class SemanticDocumentVectorStoreMapper {

    // Spring AI define la forma del Document del vector store; JazzLogs define el SemanticDocument de dominio.
    public Document toSpringAiDocument(SemanticDocument semanticDocument, String transformerVersion) {
        return Document.builder()
                .id(semanticDocument.documentId())
                .text(semanticDocument.getEmbeddingText())
                .metadata(semanticDocument.metadata(transformerVersion))
                .build();
    }
}
