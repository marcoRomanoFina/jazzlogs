package com.marcoromanofinaa.jazzlogs.ai.recommend.flow;

import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendResponse;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TrackRecommendFlow implements RecommendFlow {

    @Override
    public AiRecommendMode supports() {
        return AiRecommendMode.TRACKS;
    }

    @Override
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        log.warn("Track recommend flow requested before implementation was enabled. question='{}'", request.question());
        throw new FeatureUnavailableException("Track recommend flow is not implemented yet");
    }
}
