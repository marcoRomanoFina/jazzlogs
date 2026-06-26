package com.marcoromanofinaa.jazzlogs.recommendation.config;

import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseTextConfig;
import java.util.Optional;
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
        String verbosity,
        String proReasoningEffort,
        String proVerbosity
) {

    public Optional<Reasoning> reasoning() {
        if (reasoningEffort == null || reasoningEffort.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                Reasoning.builder()
                        .effort(ReasoningEffort.of(reasoningEffort))
                        .build()
        );
    }

    public Optional<Double> temperatureForModel(String providerModelName) {
        if (temperature == null) {
            return Optional.empty();
        }
        if (providerModelName != null && providerModelName.equalsIgnoreCase("gpt-5-nano")) {
            return Optional.empty();
        }
        return Optional.of(temperature);
    }

    public Optional<ResponseTextConfig.Verbosity> responseVerbosity() {
        if (verbosity == null || verbosity.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ResponseTextConfig.Verbosity.of(verbosity));
    }

    public Optional<Reasoning> proReasoning() {
        if (proReasoningEffort == null || proReasoningEffort.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                Reasoning.builder()
                        .effort(ReasoningEffort.of(proReasoningEffort))
                        .build()
        );
    }

    public Optional<ResponseTextConfig.Verbosity> proResponseVerbosity() {
        if (proVerbosity == null || proVerbosity.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(ResponseTextConfig.Verbosity.of(proVerbosity));
    }
}
