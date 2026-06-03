package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.recommendation.config.AIModelCatalog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendationOrchestrator {

    private final AIModelCatalog aiModelCatalog;
    private final RecommendationFlowResolver recommendationFlowResolver;

    public RecommendationResult generate(RecommendationCommand command) {
        var modelDefinition = aiModelCatalog.getModel(command.requestedModel());
        var flow = recommendationFlowResolver.resolve(modelDefinition.recommendationFlowType());
        return flow.generate(new RecommendationFlowCommand(
                command.userId(),
                command.chatSessionId(),
                command.userMessage(),
                command.timeZone(),
                command.recommendationMemory(),
                null,
                command.recommendationMemory() == null ? null : command.recommendationMemory().sessionSummary(),
                List.of(),
                List.of(),
                command.requestedModel(),
                modelDefinition,
                command.recentHistory(),
                null
        ));
    }
}
