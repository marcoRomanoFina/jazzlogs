package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WinnerReference(
        @NotNull BasicRecommendationTarget type,
        @NotBlank String id,
        @NotBlank String name,
        @NotBlank String artistFullName
) {
}
