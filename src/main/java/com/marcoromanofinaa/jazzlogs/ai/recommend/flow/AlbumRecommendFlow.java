package com.marcoromanofinaa.jazzlogs.ai.recommend.flow;

import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendMode;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.AiRecommendResponse;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AlbumRecommendFlow implements RecommendFlow {

    @Override
    public AiRecommendMode supports() {
        return AiRecommendMode.ALBUM;
    }

    @Override
    public AiRecommendResponse recommend(AiRecommendRequest request) {
        log.warn("Album recommend flow requested before implementation was enabled. question='{}'", request.question());
        throw new FeatureUnavailableException("Album recommend flow is not implemented yet");
    }
}
