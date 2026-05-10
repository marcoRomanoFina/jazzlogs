package com.marcoromanofinaa.jazzlogs.ai.recommend.core;

import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendRequest;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendResponse;
import com.marcoromanofinaa.jazzlogs.ai.recommend.basic.strategy.RecommendStrategyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiRecommendService {

    private final RecommendStrategyResolver strategyResolver;

    public AiRecommendResponse recommend(AiRecommendRequest request) {
        var strategy = strategyResolver.resolve(request.question());
        log.warn(
                "AI recommend routed question='{}' to strategy={}",
                request.question(),
                strategy.getClass().getSimpleName()
        );
        return strategy.recommend(request);
    }
}
