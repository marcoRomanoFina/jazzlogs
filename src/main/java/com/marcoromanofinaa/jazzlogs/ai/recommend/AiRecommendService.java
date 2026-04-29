package com.marcoromanofinaa.jazzlogs.ai.recommend;

import com.marcoromanofinaa.jazzlogs.ai.recommend.flow.RecommendFlowResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiRecommendService {

    private final RecommendFlowResolver flowResolver;

    public AiRecommendResponse recommend(AiRecommendRequest request) {
        var flow = flowResolver.resolve(request.question());
        log.warn(
                "AI recommend routed question='{}' to flow={}",
                request.question(),
                flow.getClass().getSimpleName()
        );
        return flow.recommend(request);
    }
}
