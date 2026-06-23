package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;

public interface JazzTool {

    JazzToolName name();

    JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context);
}
