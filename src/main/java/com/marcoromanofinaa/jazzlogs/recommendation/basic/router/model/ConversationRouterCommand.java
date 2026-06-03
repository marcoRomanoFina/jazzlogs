package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import java.util.List;
import java.util.UUID;

public record ConversationRouterCommand(
        UUID userId,
        UUID chatSessionId,
        String userMessage,
        String timeZone,
        AIModelType requestedModel,
        AIModelDefinition requestedModelDefinition,
        ChatRecommendationMemory recommendationMemory,
        List<ChatExchange> recentHistory
) {
}
