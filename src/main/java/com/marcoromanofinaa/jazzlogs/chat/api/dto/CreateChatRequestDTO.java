package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record CreateChatRequestDTO(
        @NotNull @Valid UserChatMessageDTO userMessage,
        String timeZone
) {
}
