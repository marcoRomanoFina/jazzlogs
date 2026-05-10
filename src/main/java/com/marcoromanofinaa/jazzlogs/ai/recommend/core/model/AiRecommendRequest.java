package com.marcoromanofinaa.jazzlogs.ai.recommend.core.model;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendRequest(
        @NotBlank
        String question
) {
}
