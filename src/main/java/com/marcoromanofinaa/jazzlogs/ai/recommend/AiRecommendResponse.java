package com.marcoromanofinaa.jazzlogs.ai.recommend;

import java.util.List;

public record AiRecommendResponse(
        String question,
        AiRecommendMode mode,
        String answer,
        List<AiRecommendItem> recommendations,
        List<String> sources
) {
}
