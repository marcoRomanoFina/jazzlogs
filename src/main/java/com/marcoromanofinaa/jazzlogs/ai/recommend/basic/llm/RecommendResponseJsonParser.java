package com.marcoromanofinaa.jazzlogs.ai.recommend.basic.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RecommendResponseJsonParser {

    private final ObjectMapper objectMapper;

    public AlbumRecommendDecision parseAlbumDecision(String rawResponse) {
        return readValue(rawResponse, AlbumRecommendDecision.class);
    }

    public TrackRecommendDecision parseTrackDecision(String rawResponse) {
        return readValue(rawResponse, TrackRecommendDecision.class);
    }

    private <T> T readValue(String rawResponse, Class<T> targetType) {
        var normalizedResponse = normalizeJsonPayload(rawResponse);
        try {
            return objectMapper.readValue(normalizedResponse, targetType);
        }
        catch (JsonProcessingException exception) {
            log.warn(
                    "AI recommend returned invalid JSON. targetType={}, rawResponse='{}', normalizedResponse='{}'",
                    targetType.getSimpleName(),
                    abbreviate(rawResponse),
                    abbreviate(normalizedResponse)
            );
            throw new FeatureUnavailableException("AI recommend returned an invalid JSON response");
        }
    }

    private String normalizeJsonPayload(String rawResponse) {
        var trimmed = stripMarkdownFences(rawResponse);
        var extractedObject = extractTopLevelJsonObject(trimmed);
        return extractedObject != null ? extractedObject : trimmed;
    }

    private String stripMarkdownFences(String rawResponse) {
        var trimmed = rawResponse == null ? "" : rawResponse.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```(?:json)?\\s*", "");
            trimmed = trimmed.replaceFirst("\\s*```$", "");
        }
        return trimmed.trim();
    }

    private String extractTopLevelJsonObject(String rawResponse) {
        var start = rawResponse.indexOf('{');
        if (start < 0) {
            return null;
        }

        var depth = 0;
        var inString = false;
        var escaping = false;

        for (var index = start; index < rawResponse.length(); index++) {
            var current = rawResponse.charAt(index);

            if (escaping) {
                escaping = false;
                continue;
            }

            if (current == '\\') {
                escaping = true;
                continue;
            }

            if (current == '"') {
                inString = !inString;
                continue;
            }

            if (inString) {
                continue;
            }

            if (current == '{') {
                depth++;
            }
            else if (current == '}') {
                depth--;
                if (depth == 0) {
                    return rawResponse.substring(start, index + 1);
                }
            }
        }

        return null;
    }

    private String abbreviate(String value) {
        if (value == null) {
            return "<null>";
        }

        var singleLine = value.replaceAll("\\s+", " ").trim();
        return singleLine.length() <= 600 ? singleLine : singleLine.substring(0, 600) + "...";
    }
}
