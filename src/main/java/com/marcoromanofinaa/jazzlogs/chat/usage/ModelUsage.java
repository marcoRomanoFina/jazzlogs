package com.marcoromanofinaa.jazzlogs.chat.usage;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;

public record ModelUsage(
        UsageRecordStage stage,
        AIModelType modelUsed,
        String providerModelName,
        Integer inputTokens,
        Integer cachedInputTokens,
        Integer outputTokens
) {

    public int totalTokens() {
        return safe(inputTokens) + safe(outputTokens);
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
