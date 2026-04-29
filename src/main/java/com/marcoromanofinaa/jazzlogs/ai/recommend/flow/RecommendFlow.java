package com.marcoromanofinaa.jazzlogs.ai.recommend.flow;

import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendResponse;

public interface RecommendFlow {

    AiRecommendMode supports();

    AiRecommendResponse recommend(AiRecommendRequest request);
}
