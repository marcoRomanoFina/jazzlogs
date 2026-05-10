package com.marcoromanofinaa.jazzlogs.ai.recommend.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.ai.recommend.openai")
public record RecommendOpenAiProperties(
        String baseUrl,
        String model,
        Double temperature,
        Integer maxCompletionTokens,
        String reasoningEffort,
        String verbosity
) {
}
