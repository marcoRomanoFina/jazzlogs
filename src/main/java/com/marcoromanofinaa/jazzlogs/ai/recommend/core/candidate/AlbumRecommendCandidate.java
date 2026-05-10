package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.UUID;

public record AlbumRecommendCandidate(
        Double similarityScore,
        String semanticDocumentId,
        UUID albumLogId,
        Integer logNumber,
        String album,
        String artist,
        AlbumRecommendDecisionContext decisionContext,
        AlbumRecommendDeliveryMetadata deliveryMetadata
) {
}
