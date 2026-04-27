package com.marcoromanofinaa.jazzlogs.ai.recommend;

import java.util.List;

public record AiRecommendResponse(
        String question,
        String answer,
        List<String> sources
) {
}
