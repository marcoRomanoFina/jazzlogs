package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import java.util.List;
import java.util.UUID;

public record RecommendationFlowCommand(
        UUID userId,
        UUID chatSessionId,
        String userMessage,
        String timeZone,
        ChatRecommendationMemory recommendationMemory,
        String contextualizedUserMessage,
        String sessionSummary,
        List<String> excludedNodeIds,
        AIModelType requestedModel,
        AIModelDefinition modelDefinition,
        List<ChatExchange> recentHistory,
        BasicRecommendationTarget recommendationTarget
) {
}
