package com.marcoromanofinaa.jazzlogs.ai.semantic.admin;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocument;
import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;

public record SemanticDocumentPreview(
        SemanticDocumentType type,
        String sourceId,
        String title,
        String embeddingText
) {

    public static SemanticDocumentPreview from(SemanticDocument document) {
        return new SemanticDocumentPreview(
                document.type(),
                document.getSourceId(),
                document.getTitle(),
                document.getEmbeddingText()
        );
    }
}
