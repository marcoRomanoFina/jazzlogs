package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolName;
import java.util.Map;

public record JazzAgentToolCallRequest(
        String callId,
        JazzToolName toolName,
        Map<String, Object> arguments
) {

    public JazzAgentToolCallRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
