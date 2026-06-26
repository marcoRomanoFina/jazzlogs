package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import java.util.List;
import java.util.UUID;

public record JazzAgentContext(
        UUID userId,
        UUID chatSessionId,
        String userMessage,
        String timeZone,
        ChatRecommendationMemory recommendationMemory,
        List<ChatExchange> recentHistory,
        AIModelDefinition modelDefinition
) {
}
