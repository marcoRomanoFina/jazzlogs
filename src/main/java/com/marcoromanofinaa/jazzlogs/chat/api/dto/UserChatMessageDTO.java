package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UserChatMessageDTO(
        @NotBlank String content,
        @NotNull AIModelType requestedModel
) {
}
