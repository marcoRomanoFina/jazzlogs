package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

public record RecommendationTiming(
        long routerLatencyMs,
        long flowLatencyMs,
        long totalRecommendationLatencyMs
) {
}
