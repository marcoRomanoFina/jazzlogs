package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.RecommendationFlowNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationFlowResolver {

    private final List<RecommendationFlow> flows;

    public RecommendationFlow resolve(RecommendationFlowType flowType) {
        return flows.stream()
                .filter(flow -> flow.flowType() == flowType)
                .findFirst()
                .orElseThrow(() -> new RecommendationFlowNotFoundException(flowType));
    }
}
