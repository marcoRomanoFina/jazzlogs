package com.marcoromanofinaa.jazzlogs.recommendation.orchestration;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record RecommendationResult(
        @NotBlank String assistantResponse,
        @NotNull List<@NotNull WinnerReference> winners,
        BasicRecommendationTarget recommendationType,
        String suggestedChatTitle,
        String updatedSessionSummary,
        RecommendationTiming timing,
        @NotNull @Size(min = 1) List<@NotNull ModelUsage> usageEntries
) {

    public static RecommendationResult fromAssistantResponse(
            String assistantResponse,
            List<ModelUsage> usageEntries
    ) {
        return new RecommendationResult(
                assistantResponse,
                List.of(),
                null,
                null,
                null,
                new RecommendationTiming(0L, 0L, 0L),
                usageEntries
        );
    }

    public AIModelType modelUsed() {
        return lastUsage().modelUsed();
    }

    public Integer inputTokens() {
        return usageEntries.stream().mapToInt(u -> safe(u.inputTokens())).sum();
    }

    public Integer cachedInputTokens() {
        return usageEntries.stream().mapToInt(u -> safe(u.cachedInputTokens())).sum();
    }

    public Integer outputTokens() {
        return usageEntries.stream().mapToInt(u -> safe(u.outputTokens())).sum();
    }

    private ModelUsage lastUsage() {
        if (usageEntries == null || usageEntries.isEmpty()) {
            throw new IllegalStateException("Cannot determine model usage: no usage entries found");
        }
        return usageEntries.getLast();
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
