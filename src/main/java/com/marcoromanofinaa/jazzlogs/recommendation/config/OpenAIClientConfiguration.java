package com.marcoromanofinaa.jazzlogs.recommendation.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenAIClientConfiguration {

    @Bean
    public OpenAIClient openAIClient(
            @Value("${spring.ai.openai.api-key:}") String apiKey,
            OpenAIRecommendationProperties properties
    ) {
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(normalizeBaseUrl(properties.baseUrl()))
                .build();
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }

        var normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (normalized.endsWith("/v1")) {
            return normalized;
        }
        return normalized + "/v1";
    }
}
