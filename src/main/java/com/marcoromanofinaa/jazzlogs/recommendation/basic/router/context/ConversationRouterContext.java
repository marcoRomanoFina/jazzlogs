package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.context;

import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import java.util.List;

public record ConversationRouterContext(
        String userDisplayName,
        String userPreferencesSummary,
        String lastAssistantMessage,
        String recentExchangesSummary,
        String existingSessionSummary,
        ChatRecommendationMemory.LastRecommendedItem lastRecommendedItem,
        List<ChatRecommendationMemory.OrderedRecommendedItem> orderedRecommendedItems
) {
}
