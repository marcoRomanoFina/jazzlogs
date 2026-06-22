package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatExchangeDTO(
        UUID id,
        String userMessage,
        AIModelType requestedModel,
        String assistantResponse,
        BasicRecommendationTarget recommendationType,
        List<RecommendedItemDTO> recommendedItems,
        AIModelType modelUsed,
        Instant createdAt
) {
}
