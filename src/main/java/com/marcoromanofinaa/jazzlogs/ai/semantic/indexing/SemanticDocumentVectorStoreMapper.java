package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
public class SemanticDocumentVectorStoreMapper {

    // Spring AI define la forma del Document del vector store; JazzLogs define el SemanticDocument de dominio.
    public Document toSpringAiDocument(SemanticDocument semanticDocument, String transformerVersion) {
        var semanticDocumentId = semanticDocument.documentId();
        return Document.builder()
                .id(toVectorStoreId(semanticDocumentId))
                .text(semanticDocument.getEmbeddingText())
                .metadata(enrichMetadata(semanticDocument, transformerVersion, semanticDocumentId))
                .build();
    }

    private String toVectorStoreId(String semanticDocumentId) {
        return UUID.nameUUIDFromBytes(semanticDocumentId.getBytes(StandardCharsets.UTF_8)).toString();
    }

    private java.util.Map<String, Object> enrichMetadata(
            SemanticDocument semanticDocument,
            String transformerVersion,
            String semanticDocumentId
    ) {
        var metadata = semanticDocument.metadata(transformerVersion);
        metadata.put("semanticDocumentId", semanticDocumentId);
        return metadata;
    }
}
