package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.preferences.UserPreferencesContext;
import java.time.ZonedDateTime;
import java.util.List;

public record BasicPromptCommand(
        String userMessage,
        BasicRecommendationTarget target,
        List<ChatExchange> recentHistory,
        String sessionSummary,
        ZonedDateTime currentLocalTime,
        UserPreferencesContext userPreferencesContext,
        List<RecommendationCandidate> candidates
) {
}
