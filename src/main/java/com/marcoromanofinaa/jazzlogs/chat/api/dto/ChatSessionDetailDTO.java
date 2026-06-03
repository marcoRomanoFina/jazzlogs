package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ChatSessionDetailDTO(
        UUID chatSessionId,
        String title,
        List<ChatExchangeDTO> exchanges,
        Instant lastInteractionAt,
        Instant createdAt
) {
}
