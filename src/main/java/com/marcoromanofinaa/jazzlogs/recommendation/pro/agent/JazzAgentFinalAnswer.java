package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record JazzAgentFinalAnswer(
        @NotNull JazzAgentResultType resultType,
        @NotBlank String assistantResponse,
        @NotNull List<@Valid @NotNull WinnerReference> winners,
        BasicRecommendationTarget recommendationType,
        String suggestedChatTitle,
        String updatedSessionSummary
) {

    public JazzAgentFinalAnswer {
        winners = winners == null ? List.of() : List.copyOf(winners);
    }

    @AssertTrue(message = "must include winners and recommendationType for MUSIC_RECOMMENDATION")
    boolean hasRecommendationPayloadWhenNeeded() {
        return resultType != JazzAgentResultType.MUSIC_RECOMMENDATION
                || !winners.isEmpty() && recommendationType != null;
    }

    @AssertTrue(message = "must not include winners or recommendationType for DIRECT_RESPONSE")
    boolean hasDirectResponseConsistency() {
        return resultType != JazzAgentResultType.DIRECT_RESPONSE
                || winners.isEmpty() && recommendationType == null;
    }
}
