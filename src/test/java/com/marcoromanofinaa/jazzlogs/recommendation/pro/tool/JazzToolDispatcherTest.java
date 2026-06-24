package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class JazzToolDispatcherTest {

    @Test
    void dispatchRoutesCallToRegisteredTool() {
        var dispatcher = new JazzToolDispatcher(List.of(new DummyTool()));

        var result = dispatcher.dispatch(
                new JazzToolCall(JazzToolName.ALBUM_CONTEXT, Map.of("albumNodeId", "123")),
                new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null)
        );

        assertThat(result.toolName()).isEqualTo(JazzToolName.ALBUM_CONTEXT);
        assertThat(result.content()).isEqualTo("ok");
    }

    @Test
    void dispatchFailsForUnknownTool() {
        var dispatcher = new JazzToolDispatcher(List.of());

        assertThatThrownBy(() -> dispatcher.dispatch(
                new JazzToolCall(JazzToolName.ALBUM_CONTEXT, Map.of()),
                new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("No Jazz tool registered");
    }

    @Test
    void dispatchFailsWithoutContext() {
        var dispatcher = new JazzToolDispatcher(List.of(new DummyTool()));

        assertThatThrownBy(() -> dispatcher.dispatch(
                new JazzToolCall(JazzToolName.ALBUM_CONTEXT, Map.of()),
                null
        ))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void constructorFailsForDuplicateToolNames() {
        assertThatThrownBy(() -> new JazzToolDispatcher(List.of(new DummyTool(), new DummyTool())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate Jazz tool registered");
    }

    @Test
    void toolCallDefaultsArgumentsToEmptyImmutableMap() {
        var call = new JazzToolCall(JazzToolName.ALBUM_CONTEXT, null);

        assertThat(call.arguments()).isEmpty();
        assertThatThrownBy(() -> call.arguments().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void executionResultDefaultsMetadataToEmptyImmutableMap() {
        var result = new JazzToolExecutionResult(JazzToolName.ALBUM_CONTEXT, "ok", null);

        assertThat(result.metadata()).isEmpty();
        assertThatThrownBy(() -> result.metadata().put("x", "y"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static class DummyTool implements JazzTool {

        @Override
        public JazzToolName name() {
            return JazzToolName.ALBUM_CONTEXT;
        }

        @Override
        public String description() {
            return "Dummy album context tool";
        }

        @Override
        public Map<String, Object> parametersSchema() {
            return Map.of(
                    "type", "object",
                    "additionalProperties", false,
                    "properties", Map.of(),
                    "required", List.of()
            );
        }

        @Override
        public JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context) {
            return new JazzToolExecutionResult(call.toolName(), "ok", Map.of());
        }
    }
}
