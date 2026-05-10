package com.marcoromanofinaa.jazzlogs.ai.recommend.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class RecommendResponsesClient {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final RestClient restClient;
    private final RecommendOpenAiProperties properties;

    public RecommendResponsesClient(
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder,
            RecommendOpenAiProperties properties,
            @Value("${spring.ai.openai.api-key:}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .build();
    }

    public RecommendResponsesResult call(Prompt prompt) {
        var options = (OpenAiChatOptions) prompt.getOptions();
        var requestBody = buildRequestBody(prompt, options);

        try {
            var rawJson = restClient.post()
                    .uri("/v1/responses")
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            return parseResult(rawJson);
        }
        catch (RestClientResponseException exception) {
            log.warn(
                    "Responses API recommend call failed. status={}, body={}",
                    exception.getStatusCode(),
                    abbreviate(exception.getResponseBodyAsString())
            );
            throw exception;
        }
    }

    private Map<String, Object> buildRequestBody(Prompt prompt, OpenAiChatOptions options) {
        var requestBody = new LinkedHashMap<String, Object>();
        requestBody.put("model", firstNonBlank(normalizeModel(options.getModel()), properties.model()));
        requestBody.put("input", List.of(
                Map.of(
                        "role", "system",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", prompt.getSystemMessage().getText()
                        ))
                ),
                Map.of(
                        "role", "user",
                        "content", List.of(Map.of(
                                "type", "input_text",
                                "text", prompt.getUserMessage().getText()
                        ))
                )
        ));

        var temperature = options.getTemperature() != null ? options.getTemperature() : properties.temperature();
        if (temperature != null) {
            requestBody.put("temperature", temperature);
        }
        var maxOutputTokens = options.getMaxCompletionTokens() != null
                ? options.getMaxCompletionTokens()
                : properties.maxCompletionTokens();
        if (maxOutputTokens != null) {
            requestBody.put("max_output_tokens", maxOutputTokens);
        }
        if (options.getPromptCacheKey() != null && !options.getPromptCacheKey().isBlank()) {
            requestBody.put("prompt_cache_key", options.getPromptCacheKey());
        }
        var reasoningEffort = firstNonBlank(options.getReasoningEffort(), properties.reasoningEffort());
        if (reasoningEffort != null) {
            requestBody.put("reasoning", Map.of("effort", reasoningEffort));
        }
        var verbosity = firstNonBlank(options.getVerbosity(), properties.verbosity());
        if (options.getResponseFormat() != null || verbosity != null) {
            var text = new LinkedHashMap<String, Object>();
            if (options.getResponseFormat() != null) {
                text.put("format", toResponsesFormat(options.getResponseFormat()));
            }
            if (verbosity != null) {
                text.put("verbosity", verbosity);
            }
            requestBody.put("text", text);
        }

        return requestBody;
    }

    private Map<String, Object> toResponsesFormat(ResponseFormat responseFormat) {
        if (responseFormat.getType() == ResponseFormat.Type.JSON_SCHEMA && responseFormat.getJsonSchema() != null) {
            var jsonSchema = responseFormat.getJsonSchema();
            var format = new LinkedHashMap<String, Object>();
            format.put("type", "json_schema");
            format.put("name", jsonSchema.getName());
            format.put("schema", jsonSchema.getSchema());
            if (jsonSchema.getStrict() != null) {
                format.put("strict", jsonSchema.getStrict());
            }
            return format;
        }

        return Map.of("type", "text");
    }

    private String normalizeModel(Object model) {
        if (model == null) {
            return null;
        }
        if (model instanceof String modelName) {
            return modelName;
        }
        return model.toString();
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        if (fallback != null && !fallback.isBlank()) {
            return fallback;
        }
        return null;
    }

    private RecommendResponsesResult parseResult(String rawJson) {
        try {
            var root = objectMapper.readTree(rawJson);
            return new RecommendResponsesResult(
                    extractOutputText(root),
                    rawJson,
                    extractFinishReason(root),
                    intValue(root.path("usage").path("input_tokens")),
                    intValue(root.path("usage").path("input_tokens_details").path("cached_tokens")),
                    intValue(root.path("usage").path("output_tokens")),
                    intValue(root.path("usage").path("total_tokens")),
                    extractNativeUsage(root.path("usage"))
            );
        }
        catch (JsonProcessingException exception) {
            log.warn("Failed to parse Responses API payload: {}", abbreviate(rawJson), exception);
            throw new IllegalStateException("Failed to parse Responses API response", exception);
        }
    }

    private String extractOutputText(JsonNode root) {
        var text = new StringBuilder();
        for (var outputItem : root.path("output")) {
            if (!"message".equals(outputItem.path("type").asText())) {
                continue;
            }
            for (var contentItem : outputItem.path("content")) {
                var type = contentItem.path("type").asText();
                if ("output_text".equals(type) || "text".equals(type)) {
                    var value = contentItem.path("text").asText(null);
                    if (value != null) {
                        text.append(value);
                    }
                }
            }
        }
        return text.isEmpty() ? null : text.toString();
    }

    private String extractFinishReason(JsonNode root) {
        var status = root.path("status").asText(null);
        if ("completed".equalsIgnoreCase(status)) {
            return "STOP";
        }

        var incompleteReason = root.path("incomplete_details").path("reason").asText(null);
        if (incompleteReason != null && !incompleteReason.isBlank()) {
            return incompleteReason.toUpperCase();
        }

        return status == null ? null : status.toUpperCase();
    }

    private Map<String, Object> extractNativeUsage(JsonNode usageNode) {
        if (usageNode == null || usageNode.isMissingNode() || usageNode.isNull()) {
            return null;
        }
        return objectMapper.convertValue(usageNode, MAP_TYPE);
    }

    private Integer intValue(JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asInt();
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }

        var singleLine = value.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 1_500 ? singleLine : singleLine.substring(0, 1_500) + "...";
    }
}
