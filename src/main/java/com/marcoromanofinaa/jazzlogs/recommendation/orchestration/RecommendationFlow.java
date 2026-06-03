package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;

public interface RecommendationFlow {

    RecommendationFlowType flowType();

    RecommendationResult generate(RecommendationFlowCommand command);
}
