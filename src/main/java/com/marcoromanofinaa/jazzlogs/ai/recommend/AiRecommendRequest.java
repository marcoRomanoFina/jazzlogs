package com.marcoromanofinaa.jazzlogs.ai.recommend;

import jakarta.validation.constraints.NotBlank;

public record AiRecommendRequest(
        @NotBlank
        String question
) {
}
