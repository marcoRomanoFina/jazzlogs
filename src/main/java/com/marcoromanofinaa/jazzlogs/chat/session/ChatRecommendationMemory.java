package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import java.util.List;

public record ChatRecommendationMemory(
        LastRecommendationBatch lastRecommendationBatch,
        List<RecommendationHistoryEntry> recommendationHistory,
        String sessionSummary
) {

    public record LastRecommendationBatch(
            List<WinnerReference> winners,
            List<ResolvedRecommendationMemoryItem> items
    ) {
    }

    public record RecommendationHistoryEntry(
            Integer order,
            WinnerReference winner,
            String primaryArtist,
            String album,
            String track
    ) {
    }
}
