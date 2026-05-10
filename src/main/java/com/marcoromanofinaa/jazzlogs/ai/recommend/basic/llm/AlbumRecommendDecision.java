package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm;

public record AlbumRecommendDecision(
        String answer,
        String chosenSemanticDocumentId,
        String reason
) {
}
