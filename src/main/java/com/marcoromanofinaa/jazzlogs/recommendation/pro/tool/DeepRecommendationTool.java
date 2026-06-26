package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class DeepRecommendationTool implements JazzTool {

    @Override
    public JazzToolName name() {
        return JazzToolName.DEEP_RECOMMENDATION;
    }

    @Override
    public String description() {
        return """
                Use this when the user needs a rich recommendation workflow that goes beyond BASIC:
                deeper album or track curation, nuanced matching against session taste, or multi-step music discovery
                that should return actual winners plus a polished Jazzlogs response.
                """;
    }

    @Override
    public Map<String, Object> parametersSchema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("request", Map.of("type", "string"));
        properties.put("focus", Map.of("type", List.of("string", "null")));
        properties.put("continuationOfPreviousLine", Map.of("type", "boolean"));

        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("request", "focus", "continuationOfPreviousLine"));
        return schema;
    }

    @Override
    public JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context) {
        throw new UnsupportedOperationException("DEEP_RECOMMENDATION tool is not implemented yet");
    }
}
