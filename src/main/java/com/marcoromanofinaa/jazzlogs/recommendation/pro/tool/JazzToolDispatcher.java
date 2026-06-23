package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class JazzToolDispatcher {

    private final Map<JazzToolName, JazzTool> toolsByName;

    public JazzToolDispatcher(List<JazzTool> tools) {
        this.toolsByName = new EnumMap<>(JazzToolName.class);
        for (var tool : tools) {
            if (tool != null) {
                var previous = toolsByName.put(tool.name(), tool);
                if (previous != null) {
                    throw new IllegalStateException("Duplicate Jazz tool registered for " + tool.name());
                }
            }
        }
    }

    public JazzToolExecutionResult dispatch(JazzToolCall call, JazzAgentContext context) {
        if (call == null || call.toolName() == null) {
            throw new IllegalArgumentException("Jazz tool call requires toolName");
        }
        Objects.requireNonNull(context, "Jazz tool call requires context");

        var tool = toolsByName.get(call.toolName());
        if (tool == null) {
            throw new IllegalArgumentException("No Jazz tool registered for " + call.toolName());
        }

        return tool.execute(call, context);
    }

    public boolean supports(JazzToolName toolName) {
        return toolName != null && toolsByName.containsKey(toolName);
    }
}
