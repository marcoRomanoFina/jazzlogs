package com.marcoromanofinaa.jazzlogs.ai.recommend.core.model;

import java.util.List;

public record AiRecommendResponse(
        String question,
        AiRecommendMode mode,
        String answer,
        List<AiRecommendItem> recommendations,
        List<String> sources
) {
}
