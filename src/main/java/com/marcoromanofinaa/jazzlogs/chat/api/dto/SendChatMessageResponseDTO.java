package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.util.UUID;

public record SendChatMessageResponseDTO(
        UUID chatSessionId,
        ChatExchangeDTO exchange
) {
}
