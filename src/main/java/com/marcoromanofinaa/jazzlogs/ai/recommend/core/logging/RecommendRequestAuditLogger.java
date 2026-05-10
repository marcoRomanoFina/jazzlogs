package com.marcoromanofinaa.jazzlogs.ai.recommend.core.logging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendItem;
import com.marcoromanofinaa.jazzlogs.ai.recommend.core.model.AiRecommendMode;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.marcoromanofinaa.jazzlogs.ai.recommend.openai.RecommendResponsesResult;

@Component
@Slf4j
public class RecommendRequestAuditLogger {

    private final ObjectMapper objectMapper;
    private final Path logFile;
    private final Object lock = new Object();

    public RecommendRequestAuditLogger(
            ObjectMapper objectMapper,
            @Value("${jazzlogs.ai.recommend.audit-log-path:logs/ai-recommend-requests.ndjson}") String logFilePath
    ) {
        this.objectMapper = objectMapper;
        this.logFile = Path.of(logFilePath);
    }

    public void logRequest(
            AiRecommendMode mode,
            String question,
            int candidateCount,
            Object candidates,
            RecommendResponsesResult responsesResult,
            String rawResponse,
            String answer,
            List<String> sources,
            List<AiRecommendItem> recommendations
    ) {
        logRequest(
                mode,
                question,
                candidateCount,
                candidates,
                responsesResult,
                rawResponse,
                answer,
                sources,
                recommendations,
                false,
                null,
                null
        );
    }

    public void logRequest(
            AiRecommendMode mode,
            String question,
            int candidateCount,
            Object candidates,
            RecommendResponsesResult responsesResult,
            String rawResponse,
            String answer,
            List<String> sources,
            List<AiRecommendItem> recommendations,
            boolean fallback,
            String failureCategory,
            String failureMessage
    ) {
        try {
            var parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            var payload = new LinkedHashMap<String, Object>();
            payload.put("timestamp", Instant.now().toString());
            payload.put("mode", mode.name());
            payload.put("question", question);
            payload.put("candidateCount", candidateCount);
            payload.put("candidates", candidates);
            payload.put("rawResponseLength", rawResponse == null ? null : rawResponse.length());
            payload.put("finishReason", responsesResult == null ? null : responsesResult.finishReason());
            payload.put("promptTokens", responsesResult == null ? null : responsesResult.promptTokens());
            payload.put("cachedTokens", responsesResult == null ? null : responsesResult.cachedTokens());
            payload.put("completionTokens", responsesResult == null ? null : responsesResult.completionTokens());
            payload.put("totalTokens", responsesResult == null ? null : responsesResult.totalTokens());
            payload.put("nativeUsage", responsesResult == null ? null : responsesResult.nativeUsage());
            payload.put("fallback", fallback);
            payload.put("failureCategory", failureCategory);
            payload.put("failureMessage", failureMessage);
            payload.put("rawResponsePreview", abbreviate(rawResponse));
            payload.put("answer", answer);
            payload.put("sources", sources);
            payload.put("recommendations", recommendations);

            var line = objectMapper.writeValueAsString(payload) + System.lineSeparator();
            synchronized (lock) {
                Files.writeString(
                        logFile,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            }
        }
        catch (IOException exception) {
            log.warn("Failed to append AI recommend audit log entry to {}", logFile, exception);
        }
    }

    public void logRequest(
            AiRecommendMode mode,
            String question,
            int candidateCount,
            Object candidates,
            ChatResponse chatResponse,
            String rawResponse,
            String answer,
            List<String> sources,
            List<AiRecommendItem> recommendations
    ) {
        logRequest(
                mode,
                question,
                candidateCount,
                candidates,
                chatResponse,
                rawResponse,
                answer,
                sources,
                recommendations,
                false,
                null,
                null
        );
    }

    public void logRequest(
            AiRecommendMode mode,
            String question,
            int candidateCount,
            Object candidates,
            ChatResponse chatResponse,
            String rawResponse,
            String answer,
            List<String> sources,
            List<AiRecommendItem> recommendations,
            boolean fallback,
            String failureCategory,
            String failureMessage
    ) {
        try {
            var parent = logFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            var usage = chatResponse == null || chatResponse.getMetadata() == null
                    ? null
                    : chatResponse.getMetadata().getUsage();
            var result = chatResponse == null ? null : chatResponse.getResult();
            var generationMetadata = result == null ? null : result.getMetadata();

            var payload = new LinkedHashMap<String, Object>();
            payload.put("timestamp", Instant.now().toString());
            payload.put("mode", mode.name());
            payload.put("question", question);
            payload.put("candidateCount", candidateCount);
            payload.put("candidates", candidates);
            payload.put("rawResponseLength", rawResponse == null ? null : rawResponse.length());
            payload.put("finishReason", generationMetadata == null ? null : generationMetadata.getFinishReason());
            payload.put("promptTokens", usageValue(usage, Usage::getPromptTokens));
            payload.put("completionTokens", usageValue(usage, Usage::getCompletionTokens));
            payload.put("totalTokens", usageValue(usage, Usage::getTotalTokens));
            payload.put("nativeUsage", usage == null ? null : usage.getNativeUsage());
            payload.put("fallback", fallback);
            payload.put("failureCategory", failureCategory);
            payload.put("failureMessage", failureMessage);
            payload.put("rawResponsePreview", abbreviate(rawResponse));
            payload.put("answer", answer);
            payload.put("sources", sources);
            payload.put("recommendations", recommendations);

            var line = objectMapper.writeValueAsString(payload) + System.lineSeparator();
            synchronized (lock) {
                Files.writeString(
                        logFile,
                        line,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
            }
        } catch (IOException exception) {
            log.warn("Failed to append AI recommend audit log entry to {}", logFile, exception);
        }
    }

    private Integer usageValue(Usage usage, java.util.function.Function<Usage, Integer> getter) {
        return usage == null ? null : getter.apply(usage);
    }

    private String abbreviate(String value) {
        if (value == null) {
            return null;
        }

        var singleLine = value.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 1_500 ? singleLine : singleLine.substring(0, 1_500) + "...";
    }
}
