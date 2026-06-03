package com.marcoromanofinaa.jazzlogs.recommendation.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.recommendation.AIProvider;
import com.marcoromanofinaa.jazzlogs.recommendation.config.OpenAIRecommendationProperties;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.LLMProviderUnavailableException;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.UnsupportedLLMProviderException;
import com.openai.client.OpenAIClient;
import com.openai.errors.OpenAIException;
import com.openai.errors.OpenAIIoException;
import com.openai.errors.OpenAIRetryableException;
import com.openai.errors.RateLimitException;
import com.openai.models.Reasoning;
import com.openai.models.ReasoningEffort;
import com.openai.models.responses.ResponseCreateParams;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseTextConfig;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIResponsesClient implements LLMClient {

    private final OpenAIClient openAIClient;
    private final OpenAIRecommendationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    public LLMResult generate(LLMCommand command) {
        if (command.modelDefinition().provider() != AIProvider.OPENAI) {
            throw new UnsupportedLLMProviderException(command.modelDefinition().provider());
        }

        try {
            var response = openAIClient.responses().create(buildRequest(command));
            logRawResponseIfEnabled("text", command.modelDefinition().providerModelName(), response);
            var content = response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(part -> part.outputText().stream())
                    .map(com.openai.models.responses.ResponseOutputText::text)
                    .filter(Objects::nonNull)
                    .filter(text -> !text.isBlank())
                    .reduce((left, right) -> left + "\n" + right)
                    .orElse("");
            var usage = response.usage().orElse(null);

            return new LLMResult(
                    content,
                    command.modelDefinition().type(),
                    command.modelDefinition().providerModelName(),
                    usage == null ? 0 : Math.toIntExact(usage.inputTokens()),
                    usage == null ? 0 : cachedInputTokens(usage),
                    usage == null ? 0 : Math.toIntExact(usage.outputTokens())
            );
        } catch (RateLimitException | OpenAIRetryableException | OpenAIIoException exception) {
            throw new LLMProviderUnavailableException("OpenAI is temporarily unavailable", exception);
        } catch (OpenAIException exception) {
            log.error(
                    "OpenAI request failed for model={}: {}",
                    command.modelDefinition().providerModelName(),
                    exception.getMessage(),
                    exception
            );
            throw new LLMProviderException(
                    "OpenAI request failed for model " + command.modelDefinition().providerModelName()
                            + ": " + safeMessage(exception),
                    exception
            );
        } catch (RuntimeException exception) {
            throw new LLMProviderException("Unexpected OpenAI generation failure", exception);
        }
    }

    @Override
    public <T> StructuredLLMResult<T> generateStructured(StructuredLLMCommand<T> command) {
        if (command.modelDefinition().provider() != AIProvider.OPENAI) {
            throw new UnsupportedLLMProviderException(command.modelDefinition().provider());
        }

        try {
            var response = openAIClient.responses().create(buildStructuredRequest(command));
            logRawResponseIfEnabled("structured", command.modelDefinition().providerModelName(), response);
            var content = response.output().stream()
                    .flatMap(item -> item.message().stream())
                    .flatMap(message -> message.content().stream())
                    .flatMap(part -> part.outputText().stream())
                    .findFirst()
                    .orElseThrow(() -> new LLMProviderException("OpenAI structured response did not include parsed output"));
            var usage = response.usage().orElse(null);

            return new StructuredLLMResult<>(
                    content,
                    command.modelDefinition().type(),
                    command.modelDefinition().providerModelName(),
                    usage == null ? 0 : Math.toIntExact(usage.inputTokens()),
                    usage == null ? 0 : cachedInputTokens(usage),
                    usage == null ? 0 : Math.toIntExact(usage.outputTokens())
            );
        } catch (RateLimitException | OpenAIRetryableException | OpenAIIoException exception) {
            throw new LLMProviderUnavailableException("OpenAI is temporarily unavailable", exception);
        } catch (OpenAIException exception) {
            log.error(
                    "OpenAI structured request failed for model={} and responseType={}: {}",
                    command.modelDefinition().providerModelName(),
                    command.responseType().getSimpleName(),
                    exception.getMessage(),
                    exception
            );
            throw new LLMProviderException(
                    "OpenAI request failed for model " + command.modelDefinition().providerModelName()
                            + ": " + safeMessage(exception),
                    exception
            );
        } catch (RuntimeException exception) {
            throw new LLMProviderException("Unexpected OpenAI generation failure", exception);
        }
    }

    @Override
    public boolean supports(AIProvider provider) {
        return provider == AIProvider.OPENAI;
    }

    private ResponseCreateParams buildRequest(LLMCommand command) {
        var builder = ResponseCreateParams.builder()
                .model(command.modelDefinition().providerModelName())
                .input(command.prompt().getContents())
                .maxOutputTokens(Long.valueOf(command.modelDefinition().outputTokenLimit()));

        if (supportsTemperature(command.modelDefinition().providerModelName()) && properties.temperature() != null) {
            builder.temperature(properties.temperature());
        }

        if (properties.reasoningEffort() != null && !properties.reasoningEffort().isBlank()) {
            builder.reasoning(
                    Reasoning.builder()
                            .effort(ReasoningEffort.of(properties.reasoningEffort()))
                            .build()
            );
        }

        if (properties.verbosity() != null && !properties.verbosity().isBlank()) {
            builder.text(
                    ResponseTextConfig.builder()
                            .verbosity(ResponseTextConfig.Verbosity.of(properties.verbosity()))
                            .build()
            );
        }

        return builder.build();
    }

    private <T> StructuredResponseCreateParams<T> buildStructuredRequest(StructuredLLMCommand<T> command) {
        var builder = StructuredResponseCreateParams.<T>builder()
                .model(command.modelDefinition().providerModelName())
                .input(command.prompt().getContents())
                .maxOutputTokens(Long.valueOf(command.modelDefinition().outputTokenLimit()));

        if (supportsTemperature(command.modelDefinition().providerModelName()) && properties.temperature() != null) {
            builder.temperature(properties.temperature());
        }

        var textConfigBuilder = StructuredResponseTextConfig.<T>builder()
                .format(command.responseType());

        if (properties.reasoningEffort() != null && !properties.reasoningEffort().isBlank()) {
            builder.reasoning(
                    Reasoning.builder()
                            .effort(ReasoningEffort.of(properties.reasoningEffort()))
                            .build()
            );
        }

        if (properties.verbosity() != null && !properties.verbosity().isBlank()) {
            textConfigBuilder.verbosity(ResponseTextConfig.Verbosity.of(properties.verbosity()));
        }

        builder.text(textConfigBuilder.build());

        return builder.build();
    }

    private int cachedInputTokens(com.openai.models.responses.ResponseUsage usage) {
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

    private boolean supportsTemperature(String providerModelName) {
        return providerModelName == null || !providerModelName.equalsIgnoreCase("gpt-5-nano");
    }

    private void logRawResponseIfEnabled(String mode, String providerModelName, Object response) {
        if (!Boolean.TRUE.equals(properties.rawResponseLoggingEnabled())) {
            return;
        }

        log.info(
                "OpenAI raw {} response for model={}: {}",
                mode,
                providerModelName,
                buildDebugResponse(response)
        );
    }

    private String buildDebugResponse(Object response) {
        var payload = new LinkedHashMap<String, Object>();
        payload.put("sdkToString", String.valueOf(response));

        tryAddResponseFields(payload, response);

        var lines = new ArrayList<String>();
        lines.add("");
        lines.add("=== OpenAI Raw Response ===");
        lines.add("id: " + payload.getOrDefault("id", "<unavailable>"));
        lines.add("status: " + payload.getOrDefault("status", "<unavailable>"));
        lines.add("usage: " + summarizeUsage(payload.get("usage")));
        appendTextPreview(lines, payload.get("output"));
        appendParsedJsonPreview(lines, payload.get("output"));
        appendIfPresent(lines, "error: " + safeRenderedValue(payload.get("error")));
        appendIfPresent(lines, "incompleteDetails: " + safeRenderedValue(payload.get("incompleteDetails")));
        lines.add("sdkClass: " + payload.getOrDefault("sdkClass", "<unavailable>"));
        lines.add("==========================");

        try {
            return String.join("\n", lines);
        } catch (Exception exception) {
            log.warn("Failed to render pretty OpenAI debug response, falling back to payload.toString()", exception);
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
        lines.add(indent(outputText.trim()));
    }

    private void appendParsedJsonPreview(java.util.List<String> lines, Object output) {
        var outputText = extractOutputText(output);
        if (outputText == null || outputText.isBlank()) {
            return;
        }
        try {
            var parsed = objectMapper.readTree(outputText);
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
}
