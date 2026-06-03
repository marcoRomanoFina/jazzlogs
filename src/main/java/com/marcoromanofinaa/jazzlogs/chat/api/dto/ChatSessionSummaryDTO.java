package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.time.Instant;
import java.util.UUID;

public record ChatSessionSummaryDTO(
        UUID chatSessionId,
        String title,
        Instant lastInteractionAt,
        Instant createdAt
) {
}
