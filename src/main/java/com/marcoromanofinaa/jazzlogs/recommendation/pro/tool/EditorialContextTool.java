package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class EditorialContextTool implements JazzTool {

    private final List<EditorialContextLookupStrategy> lookupStrategies;

    public EditorialContextTool(List<EditorialContextLookupStrategy> lookupStrategies) {
        this.lookupStrategies = lookupStrategies == null ? List.of() : List.copyOf(lookupStrategies);
    }

    @Override
    public JazzToolName name() {
        return JazzToolName.EDITORIAL_CONTEXT;
    }

    @Override
    public String description() {
        return """
                Use this when you need JazzLogs' editorial context for a catalog entry:
                what album was featured in a given log, what caption essence or curatorial framing was sent,
                and the key contextual note attached to that entry.
                Start with lookupMode LOG_NUMBER when the user refers to a specific log or post.
                If the lookup finds a catalog item, treat that resolution as a concrete catalog album you can cite
                as a winner in a CATALOG_RESPONSE final result when appropriate.
                """;
    }

    @Override
    public Map<String, Object> parametersSchema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("lookupMode", Map.of(
                "type", "string",
                "enum", supportedLookupModes()
        ));
        properties.put("query", Map.of("type", "string"));

        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("lookupMode", "query"));
        return schema;
    }

    @Override
    public JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context) {
        var lookupMode = requiredString(call.arguments(), "lookupMode");
        var query = requiredString(call.arguments(), "query");
        return resolveStrategy(lookupMode).execute(query, context);
    }

    private String requiredString(Map<String, Object> arguments, String key) {
        var value = normalize(arguments.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("EDITORIAL_CONTEXT requires non-blank " + key);
        }
        return value;
    }

    private String normalize(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    private EditorialContextLookupStrategy resolveStrategy(String lookupMode) {
        return lookupStrategies.stream()
                .filter(Objects::nonNull)
                .filter(strategy -> strategy.supports(lookupMode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported EDITORIAL_CONTEXT lookupMode: " + lookupMode));
    }

    private List<String> supportedLookupModes() {
        return lookupStrategies.stream()
                .filter(Objects::nonNull)
                .map(EditorialContextLookupStrategy::lookupMode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
