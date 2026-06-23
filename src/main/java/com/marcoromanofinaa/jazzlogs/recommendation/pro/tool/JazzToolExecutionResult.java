package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import java.util.Map;

public record JazzToolExecutionResult(
        JazzToolName toolName,
        String content,
        Map<String, Object> metadata
) {
    public JazzToolExecutionResult {
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
