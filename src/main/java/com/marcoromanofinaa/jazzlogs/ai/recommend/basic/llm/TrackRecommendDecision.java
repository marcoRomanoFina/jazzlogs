package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm;

import java.util.List;

public record TrackRecommendDecision(
        String answer,
        List<TrackRecommendSelection> selections
) {
}
