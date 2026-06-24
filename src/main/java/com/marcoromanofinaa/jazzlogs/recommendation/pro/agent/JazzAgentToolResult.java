package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzToolExecutionResult;

public record JazzAgentToolResult(
        String callId,
        JazzToolExecutionResult executionResult
) {
}
