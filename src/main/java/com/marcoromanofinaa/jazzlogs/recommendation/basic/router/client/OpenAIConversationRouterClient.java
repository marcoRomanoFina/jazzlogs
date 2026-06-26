package com.marcoromanofinaa.jazzlogs.recommendation.basic.router.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelDefinition;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResponse;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationRouterResult;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderUnavailableException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.RecommendationGenerationException;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResponseValidator;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.RateLimitException;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseTextConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIConversationRouterClient {

    private static final int DEBUG_OUTPUT_PREVIEW_LIMIT = 4_000;

    private final OpenAIClient openAIClient;
    private final OpenAIRecommendationProperties properties;
    private final ObjectMapper objectMapper;
    private final LLMResponseValidator llmResponseValidator;
    private volatile ResponseFormatTextJsonSchemaConfig cachedRouterJsonSchema;
    private volatile ResponseTextConfig cachedTextConfig;

    public ConversationRouterResult generate(
            Prompt prompt,
            AIModelDefinition modelDefinition,
            AIModelType requestedModel
    ) {
        if (modelDefinition.provider() != AIProvider.OPENAI) {
            throw new IllegalArgumentException("Conversation router only supports OpenAI");
        }

        try {
            var response = openAIClient.responses().create(buildRequest(prompt, modelDefinition));
            logRawResponseIfEnabled(modelDefinition.providerModelName(), response);
            var parsedResponse = parseResponseContent(response);
            logParsedRouterResponse(modelDefinition.providerModelName(), parsedResponse, response);
            return new ConversationRouterResult(
                    parsedResponse,
                    java.util.List.of(buildUsageRecord(response, requestedModel, modelDefinition))
            );
        } catch (RateLimitException | OpenAIRetryableException | OpenAIIoException exception) {
            throw new LLMProviderUnavailableException("OpenAI is temporarily unavailable", exception);
        } catch (OpenAIException exception) {
            log.error(
                    "OpenAI router request failed for model={}: {}",
                    modelDefinition.providerModelName(),
                    exception.getMessage(),
                    exception
            );
            throw new LLMProviderException(
                    "OpenAI request failed for model " + modelDefinition.providerModelName() + ": " + safeMessage(exception),
                    exception
            );
        } catch (JsonProcessingException exception) {
            throw new LLMProviderException("Failed to parse OpenAI router JSON response", exception);
        } catch (RecommendationGenerationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new LLMProviderException("Unexpected OpenAI router generation failure", exception);
        }
    }

    private ResponseCreateParams buildRequest(Prompt prompt, AIModelDefinition modelDefinition) {
        var builder = ResponseCreateParams.builder()
                .model(modelDefinition.providerModelName())
                .input(prompt.getContents())
                .maxOutputTokens(Long.valueOf(modelDefinition.outputTokenLimit()))
                .text(
                        buildTextConfig()
                );

        properties.reasoning().ifPresent(builder::reasoning);

        return builder.build();
    }

    private ResponseTextConfig buildTextConfig() {
        var cached = cachedTextConfig;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (cachedTextConfig != null) {
                return cachedTextConfig;
            }

            var builder = ResponseTextConfig.builder().format(routerJsonSchema());
            properties.responseVerbosity().ifPresent(builder::verbosity);
            cachedTextConfig = builder.build();
            return cachedTextConfig;
        }
    }

    private ResponseFormatTextJsonSchemaConfig routerJsonSchema() {
        var cached = cachedRouterJsonSchema;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (cachedRouterJsonSchema != null) {
                return cachedRouterJsonSchema;
            }

            cachedRouterJsonSchema = ResponseFormatTextJsonSchemaConfig.builder()
                    .name("conversation_router")
                    .strict(true)
                    .schema(
                            ResponseFormatTextJsonSchemaConfig.Schema.builder()
                                    .additionalProperties(jsonSchema())
                                    .build()
                    )
                    .build();
            return cachedRouterJsonSchema;
        }
    }

    private Map<String, JsonValue> jsonSchema() {
        var referenceProperties = new LinkedHashMap<String, Object>();
        referenceProperties.put("type", Map.of(
                "type", "string",
                "enum", java.util.List.of("ARTIST", "ALBUM", "TRACK")
        ));
        referenceProperties.put("name", Map.of("type", "string"));

        var referenceItemSchema = new LinkedHashMap<String, Object>();
        referenceItemSchema.put("type", "object");
        referenceItemSchema.put("additionalProperties", false);
        referenceItemSchema.put("properties", referenceProperties);
        referenceItemSchema.put("required", java.util.List.of("type", "name"));

        var subgraphProperties = new LinkedHashMap<String, Object>();
        subgraphProperties.put("styles", requiredArrayOfStrings());
        subgraphProperties.put("instruments", requiredArrayOfStrings());
        subgraphProperties.put("rhythms", requiredArrayOfStrings());
        subgraphProperties.put("references", Map.of(
                "type", "array",
                "items", referenceItemSchema
        ));

        var subgraphSchema = new LinkedHashMap<String, Object>();
        subgraphSchema.put("type", "object");
        subgraphSchema.put("additionalProperties", false);
        subgraphSchema.put("properties", subgraphProperties);
        subgraphSchema.put("required", java.util.List.of("styles", "instruments", "rhythms", "references"));

        var properties = new LinkedHashMap<String, Object>();
        properties.put("route", Map.of(
                "type", "string",
                "enum", java.util.List.of("DIRECT_ANSWER", "CLARIFICATION_NEEDED", "MUSIC_RECOMMENDATION")
        ));
        properties.put("userIntent", Map.of(
                "type", "string",
                "enum", java.util.List.of(
                        "RECOMMEND_ALBUM",
                        "RECOMMEND_TRACK",
                        "FACTUAL_QUESTION",
                        "REACTION",
                        "SMALLTALK",
                        "OUT_OF_SCOPE",
                        "NONSENSE",
                        "UNKNOWN"
                )
        ));
        properties.put("isFollowUp", Map.of("type", "boolean"));
        properties.put("needsRetrieval", Map.of("type", "boolean"));
        properties.put("updatedSessionSummary", nullableString());
        properties.put("suggestedChatTitle", nullableString());
        properties.put("contextualizedQuery", nullableString());
        properties.put("directAnswer", nullableString());
        properties.put("clarificationQuestion", nullableString());
        properties.put("excludedNodeIds", nullableArrayOfStrings(null));
        properties.put("subgraphFilters", nullableObject(subgraphSchema));

        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", java.util.List.of(
                "route",
                "userIntent",
                "isFollowUp",
                "needsRetrieval",
                "updatedSessionSummary",
                "suggestedChatTitle",
                "contextualizedQuery",
                "directAnswer",
                "clarificationQuestion",
                "excludedNodeIds",
                "subgraphFilters"
        ));

        return toJsonValueMap(schema);
    }

    private Map<String, Object> nullableString() {
        return Map.of("type", java.util.List.of("string", "null"));
    }

    private Map<String, Object> nullableArrayOfStrings(java.util.List<String> enumValues) {
        var items = new LinkedHashMap<String, Object>();
        items.put("type", "string");
        if (enumValues != null) {
            items.put("enum", enumValues);
        }
        return Map.of(
                "type", java.util.List.of("array", "null"),
                "items", items
        );
    }

    private Map<String, Object> requiredArrayOfStrings() {
        return Map.of(
                "type", "array",
                "items", Map.of("type", "string")
        );
    }

    private Map<String, Object> nullableObject(Map<String, Object> objectSchema) {
        var properties = new LinkedHashMap<>(objectSchema);
        properties.put("type", java.util.List.of("object", "null"));
        return properties;
    }

    private Map<String, JsonValue> toJsonValueMap(Map<String, Object> value) {
        var result = new LinkedHashMap<String, JsonValue>();
        value.forEach((key, entryValue) -> result.put(key, JsonValue.from(entryValue)));
        return result;
    }

    private int cachedInputTokens(com.openai.models.responses.ResponseUsage usage) {
        try {
            return Math.toIntExact(usage.inputTokensDetails().cachedTokens());
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private ConversationRouterResponse parseResponseContent(com.openai.models.responses.Response response)
            throws JsonProcessingException {
        var content = response.output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(part -> part.outputText().stream())
                .map(com.openai.models.responses.ResponseOutputText::text)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElseThrow(() -> new LLMProviderException("OpenAI router response did not include text output"));

        var parsed = objectMapper.readValue(content, ConversationRouterResponse.class);
        return llmResponseValidator.validate(
                sanitize(parsed),
                ConversationRouterResponse.class
        );
    }

    private ConversationRouterResponse sanitize(ConversationRouterResponse response) {
        if (response == null || response.route() == null) {
            return response;
        }

        return switch (response.route()) {
            case DIRECT_ANSWER -> new ConversationRouterResponse(
                    response.route(),
                    response.userIntent(),
                    response.isFollowUp(),
                    false,
                    response.updatedSessionSummary(),
                    response.suggestedChatTitle(),
                    null,
                    response.directAnswer(),
                    null,
                    null,
                    null
            );
            case CLARIFICATION_NEEDED -> new ConversationRouterResponse(
                    response.route(),
                    response.userIntent(),
                    response.isFollowUp(),
                    false,
                    response.updatedSessionSummary(),
                    response.suggestedChatTitle(),
                    null,
                    null,
                    response.clarificationQuestion(),
                    null,
                    null
            );
            case MUSIC_RECOMMENDATION -> new ConversationRouterResponse(
                    response.route(),
                    response.userIntent(),
                    response.isFollowUp(),
                    true,
                    response.updatedSessionSummary(),
                    response.suggestedChatTitle(),
                    response.contextualizedQuery(),
                    null,
                    null,
                    response.excludedNodeIds(),
                    response.subgraphFilters()
            );
        };
    }

    private ModelUsage buildUsageRecord(
            com.openai.models.responses.Response response,
            AIModelType requestedModel,
            AIModelDefinition modelDefinition
    ) {
        var usage = response.usage().orElse(null);
        return new ModelUsage(
                UsageRecordStage.ROUTER,
                requestedModel,
                modelDefinition.providerModelName(),
                usage == null ? 0 : Math.toIntExact(usage.inputTokens()),
                usage == null ? 0 : cachedInputTokens(usage),
                usage == null ? 0 : Math.toIntExact(usage.outputTokens())
        );
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }

    private void logRawResponseIfEnabled(String providerModelName, Object response) {
        if (!Boolean.TRUE.equals(properties.rawResponseLoggingEnabled()) || !log.isInfoEnabled()) {
            return;
        }

        log.info(
                "OpenAI raw structured response for model={}: {}",
                providerModelName,
                buildDebugResponse(response)
        );
    }

    private void logParsedRouterResponse(
            String providerModelName,
            ConversationRouterResponse response,
            com.openai.models.responses.Response rawResponse
    ) {
        if (!log.isInfoEnabled()) {
            return;
        }

        var usage = rawResponse.usage().orElse(null);
        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("=== Router Decision ===");
        lines.add("model: " + providerModelName);
        lines.add("route: " + response.route());
        lines.add("intent: " + response.userIntent());
        lines.add("followUp: " + response.isFollowUp() + " | needsRetrieval: " + response.needsRetrieval());
        appendIfPresent(lines, "query: " + safeInline(response.contextualizedQuery()));
        appendIfPresent(lines, "title: " + safeInline(response.suggestedChatTitle()));
        appendIfPresent(lines, "sessionSummary: " + safeInline(response.updatedSessionSummary()));
        appendIfPresent(lines, "excludedNodeIds: " + safeInline(renderCompactJson(response.excludedNodeIds())));
        appendIfPresent(lines, "subgraphFilters: " + safeInline(renderCompactJson(response.subgraphFilters())));
        appendIfPresent(lines, "directAnswer: " + safeInline(response.directAnswer()));
        appendIfPresent(lines, "clarificationQuestion: " + safeInline(response.clarificationQuestion()));
        if (usage != null) {
            lines.add("usageTokens: in=%d cached=%d out=%d total=%d".formatted(
                    usage.inputTokens(),
                    cachedInputTokens(usage),
                    usage.outputTokens(),
                    usage.totalTokens()
            ));
        }
        lines.add("=======================");
        log.info(String.join("\n", lines));
    }

    private String buildDebugResponse(Object response) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("sdkToString", String.valueOf(response));

        tryAddResponseFields(payload, response);

        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("=== OpenAI Router Raw Response ===");
        lines.add("id: " + payload.getOrDefault("id", "<unavailable>"));
        lines.add("status: " + payload.getOrDefault("status", "<unavailable>"));
        lines.add("usage: " + summarizeUsage(payload.get("usage")));
        appendTextPreview(lines, payload.get("output"));
        appendParsedJsonPreview(lines, payload.get("output"));
        appendIfPresent(lines, "error: " + safeRenderedValue(payload.get("error")));
        appendIfPresent(lines, "incompleteDetails: " + safeRenderedValue(payload.get("incompleteDetails")));
        lines.add("sdkClass: " + payload.getOrDefault("sdkClass", "<unavailable>"));
        lines.add("===============================");

        try {
            return String.join("\n", lines);
        } catch (Exception exception) {
            log.warn("Failed to render pretty OpenAI router debug response, falling back to payload.toString()", exception);
            return payload.toString();
        }
    }

    private void tryAddResponseFields(LinkedHashMap<String, Object> payload, Object response) {
        try {
            var responseClass = response.getClass();
            payload.put("sdkClass", responseClass.getName());
            payload.put("id", invokeNoArg(response, "id"));
            payload.put("status", invokeNoArg(response, "status"));
            payload.put("output", invokeNoArg(response, "output"));
            payload.put("usage", invokeNoArg(response, "usage"));
            payload.put("error", invokeNoArg(response, "error"));
            payload.put("incompleteDetails", invokeNoArg(response, "incompleteDetails"));
        } catch (RuntimeException exception) {
            payload.put("fieldExtractionError", exception.getMessage());
        }
    }

    private Object invokeNoArg(Object target, String methodName) {
        try {
            var method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException exception) {
            return "<unavailable>";
        }
    }

    private String summarizeUsage(Object usage) {
        return safeRenderedValue(usage);
    }

    private void appendTextPreview(java.util.List<String> lines, Object output) {
        var outputText = extractOutputText(output);
        if (outputText == null || outputText.isBlank()) {
            return;
        }
        lines.add("outputText:");
        lines.add(indent(truncateForDebug(outputText.trim())));
    }

    private void appendParsedJsonPreview(java.util.List<String> lines, Object output) {
        var outputText = extractOutputText(output);
        if (outputText == null || outputText.isBlank()) {
            return;
        }
        try {
            var parsed = objectMapper.readTree(truncateForDebug(outputText));
            lines.add("parsedJson:");
            lines.add(indent(objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(parsed)));
        } catch (Exception ignored) {
            // Ignore non-JSON output; raw text preview is enough.
        }
    }

    private String extractOutputText(Object output) {
        if (!(output instanceof java.lang.Iterable<?> iterable)) {
            return null;
        }
        var combined = new ArrayList<String>();
        for (var item : iterable) {
            extractTextFromOutputItem(item, combined);
        }
        if (combined.isEmpty()) {
            return null;
        }
        return String.join("\n", combined);
    }

    private void extractTextFromOutputItem(Object item, java.util.List<String> combined) {
        if (item == null) {
            return;
        }
        try {
            var messageMethod = item.getClass().getMethod("message");
            var optionalMessage = messageMethod.invoke(item);
            if (!(optionalMessage instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return;
            }
            var message = optional.get();
            var contentMethod = message.getClass().getMethod("content");
            var content = contentMethod.invoke(message);
            if (!(content instanceof java.lang.Iterable<?> parts)) {
                return;
            }
            for (var part : parts) {
                extractTextFromPart(part, combined);
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort debug only.
        }
    }

    private void extractTextFromPart(Object part, java.util.List<String> combined) {
        if (part == null) {
            return;
        }
        try {
            var outputTextMethod = part.getClass().getMethod("outputText");
            var optionalText = outputTextMethod.invoke(part);
            if (!(optionalText instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return;
            }
            var outputText = optional.get();
            var textMethod = outputText.getClass().getMethod("text");
            var text = textMethod.invoke(outputText);
            if (text instanceof String string && !string.isBlank()) {
                combined.add(string);
            }
        } catch (ReflectiveOperationException ignored) {
            // Best-effort debug only.
        }
    }

    private void appendIfPresent(java.util.List<String> lines, String line) {
        if (line == null || line.isBlank() || line.endsWith("<unavailable>")) {
            return;
        }
        lines.add(line);
    }

    private String safeRenderedValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = String.valueOf(value);
        return rendered == null || rendered.isBlank() || "<unavailable>".equals(rendered) ? null : rendered;
    }

    private String indent(String value) {
        return value.lines()
                .map(line -> "  " + line)
                .reduce((left, right) -> left + "\n" + right)
                .orElse("  " + value);
    }

    private String truncateForDebug(String value) {
        if (value == null || value.length() <= DEBUG_OUTPUT_PREVIEW_LIMIT) {
            return value;
        }
        return value.substring(0, DEBUG_OUTPUT_PREVIEW_LIMIT) + "\n...[truncated]";
    }

    private String renderCompactJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return String.valueOf(value);
        }
    }

    private String safeInline(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return truncateForDebug(value.replaceAll("\\s+", " ").trim());
    }
}
