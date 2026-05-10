package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.strategy;

import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendResponse;

public interface RecommendStrategy {

    AiRecommendMode supports();

    AiRecommendResponse recommend(AiRecommendRequest request);
}
