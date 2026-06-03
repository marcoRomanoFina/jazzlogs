package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.util.UUID;

public record CreateChatResponseDTO(
        UUID chatSessionId,
        String title,
        ChatExchangeDTO exchange
) {
}
