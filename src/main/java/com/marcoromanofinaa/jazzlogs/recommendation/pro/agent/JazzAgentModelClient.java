package com.marcoromanofinaa.jazzlogs.recommendation.pro.agent;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.tool.JazzTool;
import java.util.Collection;
import java.util.List;

public interface JazzAgentModelClient {

    JazzAgentModelTurnResponse createInitialResponse(
            JazzAgentContext context,
            String systemPrompt,
            Collection<JazzTool> availableTools
    );

    JazzAgentModelTurnResponse createFollowUpResponse(
            JazzAgentContext context,
            String systemPrompt,
            String previousResponseId,
            Collection<JazzTool> availableTools,
            List<JazzAgentToolResult> toolResults
    );
}
