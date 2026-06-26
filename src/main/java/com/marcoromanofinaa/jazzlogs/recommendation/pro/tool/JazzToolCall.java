package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import java.util.Map;

public record JazzToolCall(
        JazzToolName toolName,
        Map<String, Object> arguments
) {
    public JazzToolCall {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
