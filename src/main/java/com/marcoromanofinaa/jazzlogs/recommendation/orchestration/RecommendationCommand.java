package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import java.util.List;
import java.util.UUID;

public record RecommendationCommand(
        UUID userId,
        UUID chatSessionId,
        String userMessage,
        String timeZone,
        AIModelType requestedModel,
        ChatRecommendationMemory recommendationMemory,
        List<ChatExchange> recentHistory
) {
}
