package com.marcoromanofinaa.jazzlogs.recommendation.pro;

import com.marcoromanofinaa.jazzlogs.recommendation.RecommendationFlowType;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlow;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationFlowCommand;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.RecommendationResult;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProRecommendationFlow implements RecommendationFlow {

    private final JazzAgentService jazzAgentService;

    @Override
    public RecommendationFlowType flowType() {
        return RecommendationFlowType.PRO;
    }

    @Override
    public RecommendationResult generate(RecommendationFlowCommand command) {
        return jazzAgentService.run(new JazzAgentContext(
                command.userId(),
                command.chatSessionId(),
                command.userMessage(),
                command.timeZone(),
                command.recommendationMemory(),
                command.recentHistory(),
                command.modelDefinition()
        ));
    }
}
