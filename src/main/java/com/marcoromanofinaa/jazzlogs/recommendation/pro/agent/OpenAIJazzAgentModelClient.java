package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderUnavailableException;
import com.marcoromanofinaa.jazzlogs.recommendation.llm.LLMResponseValidator;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzTool;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolName;
import com.openai.client.OpenAIClient;
import com.openai.core.JsonValue;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.RateLimitException;
import com.openai.models.responses.FunctionTool;
import com.openai.models.responses.Response;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.ResponseFormatTextJsonSchemaConfig;
import com.openai.models.responses.ResponseInputItem;
import com.openai.models.responses.ResponseOutputItem;
import com.openai.models.responses.ResponseOutputText;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.ResponseUsage;
import com.openai.models.responses.ToolChoiceOptions;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIJazzAgentModelClient implements JazzAgentModelClient {

    private final OpenAIClient openAIClient;
    private final OpenAIRecommendationProperties properties;
    private final ObjectMapper objectMapper;
    private final LLMResponseValidator llmResponseValidator;
    private volatile ResponseFormatTextJsonSchemaConfig cachedFinalAnswerJsonSchema;

    @Override
    public JazzAgentModelTurnResponse createInitialResponse(
            JazzAgentContext context,
            String systemPrompt,
            Collection<JazzTool> availableTools
    ) {
        var request = baseRequest(context, systemPrompt, availableTools)
                .input(context.userMessage())
                .build();
        return execute(request, context);
    }

    @Override
    public JazzAgentModelTurnResponse createFollowUpResponse(
            JazzAgentContext context,
            String systemPrompt,
            String previousResponseId,
            Collection<JazzTool> availableTools,
            List<JazzAgentToolResult> toolResults
    ) {
        var functionOutputs = toolResults.stream()
                .map(this::toFunctionCallOutput)
                .map(ResponseInputItem::ofFunctionCallOutput)
                .toList();

        var request = baseRequest(context, systemPrompt, availableTools)
                .previousResponseId(previousResponseId)
                .inputOfResponse(functionOutputs)
                .build();
        return execute(request, context);
    }

    private ResponseCreateParams.Builder baseRequest(
            JazzAgentContext context,
            String systemPrompt,
            Collection<JazzTool> availableTools
    ) {
        if (context.modelDefinition().provider() != AIProvider.OPENAI) {
            throw new IllegalArgumentException("Jazz agent tool calling currently supports OpenAI only");
        }

        var builder = ResponseCreateParams.builder()
                .model(context.modelDefinition().providerModelName())
                .instructions(systemPrompt)
                .maxOutputTokens(Long.valueOf(context.modelDefinition().outputTokenLimit()))
                .toolChoice(ToolChoiceOptions.AUTO)
                .parallelToolCalls(false)
                .maxToolCalls(8L);

        availableTools.stream()
                .filter(Objects::nonNull)
                .map(this::toFunctionTool)
                .forEach(builder::addTool);

        properties.reasoning().ifPresent(builder::reasoning);

        properties.temperatureForModel(context.modelDefinition().providerModelName())
                .ifPresent(builder::temperature);

        builder.text(buildFinalAnswerTextConfig(properties.responseVerbosity().orElse(null)));

        return builder;
    }

    private ResponseTextConfig buildFinalAnswerTextConfig(ResponseTextConfig.Verbosity verbosity) {
        var builder = ResponseTextConfig.builder()
                .format(finalAnswerJsonSchema());
        if (verbosity != null) {
            builder.verbosity(verbosity);
        }
        return builder.build();
    }

    private ResponseFormatTextJsonSchemaConfig finalAnswerJsonSchema() {
        var cached = cachedFinalAnswerJsonSchema;
        if (cached != null) {
            return cached;
        }

        synchronized (this) {
            if (cachedFinalAnswerJsonSchema != null) {
                return cachedFinalAnswerJsonSchema;
            }

            cachedFinalAnswerJsonSchema = ResponseFormatTextJsonSchemaConfig.builder()
                    .name("jazz_agent_final_answer")
                    .strict(true)
                    .schema(
                            ResponseFormatTextJsonSchemaConfig.Schema.builder()
                                    .additionalProperties(toJsonValueMap(finalAnswerSchema()))
                                    .build()
                    )
                    .build();
            return cachedFinalAnswerJsonSchema;
        }
    }

    private JazzAgentModelTurnResponse execute(ResponseCreateParams request, JazzAgentContext context) {
        try {
            var response = openAIClient.responses().create(request);
            var toolCalls = extractToolCalls(response.output());
            var finalAnswer = toolCalls.isEmpty() ? extractFinalAnswer(response.output()) : null;
            return new JazzAgentModelTurnResponse(
                    response.id(),
                    toolCalls,
                    finalAnswer,
                    buildUsage(response, context)
            );
        } catch (RateLimitException | OpenAIRetryableException | OpenAIIoException exception) {
            throw new LLMProviderUnavailableException("OpenAI is temporarily unavailable", exception);
        } catch (OpenAIException exception) {
            log.error(
                    "OpenAI jazz agent request failed for model={}: {}",
                    context.modelDefinition().providerModelName(),
                    exception.getMessage(),
                    exception
            );
            throw new LLMProviderException(
                    "OpenAI request failed for model " + context.modelDefinition().providerModelName() + ": " + safeMessage(exception),
                    exception
            );
        } catch (JsonProcessingException exception) {
            throw new LLMProviderException("Failed to parse OpenAI jazz agent tool arguments", exception);
        } catch (RuntimeException exception) {
            throw new LLMProviderException("Unexpected OpenAI jazz agent generation failure", exception);
        }
    }

    private FunctionTool toFunctionTool(JazzTool tool) {
        return FunctionTool.builder()
                .name(tool.name().name())
                .description(tool.description())
                .parameters(
                        FunctionTool.Parameters.builder()
                                .additionalProperties(toJsonValueMap(tool.parametersSchema()))
                                .build()
                )
                .strict(true)
                .build();
    }

    private ResponseInputItem.FunctionCallOutput toFunctionCallOutput(JazzAgentToolResult toolResult) {
        return ResponseInputItem.FunctionCallOutput.builder()
                .callId(toolResult.callId())
                .outputAsJson(Map.of(
                        "toolName", toolResult.executionResult().toolName().name(),
                        "content", toolResult.executionResult().content(),
                        "metadata", toolResult.executionResult().metadata()
                ))
                .build();
    }

    private List<JazzAgentToolCallRequest> extractToolCalls(List<ResponseOutputItem> output)
            throws JsonProcessingException {
        var toolCalls = new ArrayList<JazzAgentToolCallRequest>();
        for (var item : output) {
            if (!item.isFunctionCall()) {
                continue;
            }
            var functionCall = item.asFunctionCall();
            var toolName = parseToolName(functionCall.name());
            var arguments = objectMapper.readValue(
                    functionCall.arguments(),
                    new TypeReference<Map<String, Object>>() {
                    }
            );
            toolCalls.add(new JazzAgentToolCallRequest(
                    functionCall.callId(),
                    toolName,
                    arguments
            ));
        }
        return toolCalls;
    }

    private JazzToolName parseToolName(String rawToolName) {
        try {
            return JazzToolName.valueOf(rawToolName);
        } catch (IllegalArgumentException exception) {
            throw new LLMProviderException("OpenAI returned unsupported Jazz tool: " + rawToolName, exception);
        }
    }

    private String extractRawOutputText(List<ResponseOutputItem> output) {
        return output.stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(part -> part.outputText().stream())
                .map(ResponseOutputText::text)
                .filter(Objects::nonNull)
                .filter(text -> !text.isBlank())
                .findFirst()
                .orElseThrow(() -> new LLMProviderException("OpenAI jazz agent final response did not include JSON output"));
    }

    private JazzAgentFinalAnswer extractFinalAnswer(List<ResponseOutputItem> output) throws JsonProcessingException {
        var parsed = objectMapper.readValue(extractRawOutputText(output), JazzAgentFinalAnswer.class);
        return llmResponseValidator.validate(parsed, JazzAgentFinalAnswer.class);
    }

    private ModelUsage buildUsage(Response response, JazzAgentContext context) {
        var usage = response.usage().orElse(null);
        return new ModelUsage(
                UsageRecordStage.AGENT,
                AIModelType.PRO,
                context.modelDefinition().providerModelName(),
                usage == null ? 0 : Math.toIntExact(usage.inputTokens()),
                usage == null ? 0 : cachedInputTokens(usage),
                usage == null ? 0 : Math.toIntExact(usage.outputTokens())
        );
    }

    private Map<String, JsonValue> toJsonValueMap(Map<String, Object> value) {
        var result = new LinkedHashMap<String, JsonValue>();
        value.forEach((key, entryValue) -> result.put(key, JsonValue.from(entryValue)));
        return result;
    }

    private Map<String, Object> finalAnswerSchema() {
        var winnerProperties = new LinkedHashMap<String, Object>();
        winnerProperties.put("type", Map.of(
                "type", "string",
                "enum", List.of(
                        BasicRecommendationTarget.ALBUM.name(),
                        BasicRecommendationTarget.TRACKS.name()
                )
        ));
        winnerProperties.put("id", Map.of("type", "string"));
        winnerProperties.put("name", Map.of("type", "string"));
        winnerProperties.put("artistFullName", Map.of("type", "string"));

        var winnerSchema = new LinkedHashMap<String, Object>();
        winnerSchema.put("type", "object");
        winnerSchema.put("additionalProperties", false);
        winnerSchema.put("properties", winnerProperties);
        winnerSchema.put("required", List.of("type", "id", "name", "artistFullName"));

        var properties = new LinkedHashMap<String, Object>();
        properties.put("resultType", Map.of(
                "type", "string",
                "enum", List.of(
                        JazzAgentResultType.MUSIC_RECOMMENDATION.name(),
                        JazzAgentResultType.DIRECT_RESPONSE.name()
                )
        ));
        properties.put("assistantResponse", Map.of("type", "string"));
        properties.put("winners", Map.of(
                "type", "array",
                "items", winnerSchema
        ));
        properties.put("recommendationType", Map.of(
                "type", List.of("string", "null"),
                "enum", List.of(
                        BasicRecommendationTarget.ALBUM.name(),
                        BasicRecommendationTarget.TRACKS.name(),
                        null
                )
        ));
        properties.put("suggestedChatTitle", Map.of("type", List.of("string", "null")));
        properties.put("updatedSessionSummary", Map.of("type", List.of("string", "null")));

        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of(
                "resultType",
                "assistantResponse",
                "winners",
                "recommendationType",
                "suggestedChatTitle",
                "updatedSessionSummary"
        ));
        return schema;
    }

    private int cachedInputTokens(ResponseUsage usage) {
        try {
            return Math.toIntExact(usage.inputTokensDetails().cachedTokens());
        } catch (RuntimeException exception) {
            return 0;
        }
    }

    private String safeMessage(Exception exception) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
