package com.marcoromanofinaa.jazzlogs.ai.semantic.indexing;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;

public record SemanticIndexingRequest(
        SemanticDocumentType type,
        String sourceIdentifier
) {
}
