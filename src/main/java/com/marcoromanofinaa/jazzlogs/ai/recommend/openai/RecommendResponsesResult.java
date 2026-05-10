package com.marcoromanofinaa.jazzlogs.ai.recommend.openai;

import java.util.Map;

public record RecommendResponsesResult(
        String outputText,
        String rawJson,
        String finishReason,
        Integer promptTokens,
        Integer cachedTokens,
        Integer completionTokens,
        Integer totalTokens,
        Map<String, Object> nativeUsage
) {
}
