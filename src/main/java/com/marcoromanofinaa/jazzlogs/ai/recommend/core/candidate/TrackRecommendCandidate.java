package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.UUID;

public record TrackRecommendCandidate(
        Double similarityScore,
        String semanticDocumentId,
        UUID trackNoteId,
        String spotifyTrackId,
        String track,
        String album,
        String artist,
        Integer logNumber,
        TrackRecommendDecisionContext decisionContext,
        TrackRecommendDeliveryMetadata deliveryMetadata
) {
}
