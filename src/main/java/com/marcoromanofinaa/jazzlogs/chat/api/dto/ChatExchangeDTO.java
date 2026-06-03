package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatExchangeDTO(
        UUID id,
        String userMessage,
        AIModelType requestedModel,
        String assistantResponse,
        List<String> winners,
        List<RecommendedItemDTO> recommendedItems,
        AIModelType modelUsed,
        Instant createdAt
) {
}
