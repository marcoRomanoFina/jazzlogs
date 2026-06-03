package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;

public record StructuredLLMResult<T>(
        T content,
        AIModelType modelUsed,
        String providerModelName,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens
) {
}
