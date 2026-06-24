package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import java.util.List;

public record JazzAgentModelTurnResponse(
        String responseId,
        List<JazzAgentToolCallRequest> toolCalls,
        JazzAgentFinalAnswer finalAnswer,
        ModelUsage usage
) {

    public JazzAgentModelTurnResponse {
        toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
    }

    public boolean hasToolCalls() {
        return !toolCalls.isEmpty();
    }

    public boolean hasFinalAnswer() {
        return finalAnswer != null;
    }
}
