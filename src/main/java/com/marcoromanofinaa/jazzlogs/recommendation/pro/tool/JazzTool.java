package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.Map;

public interface JazzTool {

    JazzToolName name();

    String description();

    Map<String, Object> parametersSchema();

    JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context);
}
