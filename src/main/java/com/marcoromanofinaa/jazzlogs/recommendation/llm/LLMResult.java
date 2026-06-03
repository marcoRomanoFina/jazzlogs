package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;

public record LLMResult(
        String content,
        AIModelType modelUsed,
        String providerModelName,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens
) {
}
