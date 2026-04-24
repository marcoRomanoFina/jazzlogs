package com.marcoromanofinaa.jazzlogs.ai.ask;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record AiAskRequest(
        @NotBlank
        String question,
        @Min(1)
        @Max(20)
        Integer topK
) {
    public int resolvedTopK() {
        return topK == null ? 5 : topK;
    }
}
