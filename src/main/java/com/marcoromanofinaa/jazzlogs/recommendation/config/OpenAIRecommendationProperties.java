package com.marcoromanofinaa.jazzlogs.recommendation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.ai.recommend.openai")
public record OpenAIRecommendationProperties(
        String baseUrl,
        String model,
        String routerModel,
        Double temperature,
        Integer maxCompletionTokens,
        Integer routerMaxCompletionTokens,
        Boolean rawResponseLoggingEnabled,
        String reasoningEffort,
        String verbosity
) {
}
