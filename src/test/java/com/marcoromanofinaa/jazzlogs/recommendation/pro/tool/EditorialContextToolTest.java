package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EditorialContextToolTest {

    private static final JazzAgentContext CONTEXT =
            new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null);

    @Test
    void exposesDeclaredNameAndSchema() {
        var strategy = mock(EditorialContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new EditorialContextTool(List.of(strategy));

        assertThat(tool.name()).isEqualTo(JazzToolName.EDITORIAL_CONTEXT);
        assertThat(tool.description()).contains("editorial context");
        assertThat(tool.parametersSchema()).containsKey("properties");
        assertThat(tool.parametersSchema()).extractingByKey("properties").asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsKey("lookupMode");
    }

    @Test
    void delegatesToMatchingStrategy() {
        var strategy = mock(EditorialContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        when(strategy.supports("LOG_NUMBER")).thenReturn(true);
        when(strategy.execute("17", CONTEXT)).thenReturn(new JazzToolExecutionResult(
                JazzToolName.EDITORIAL_CONTEXT,
                "ok",
                Map.of("found", true, "id", "album-node-1")
        ));

        var tool = new EditorialContextTool(List.of(strategy));
        var result = tool.execute(
                new JazzToolCall(JazzToolName.EDITORIAL_CONTEXT, Map.of(
                        "lookupMode", "LOG_NUMBER",
                        "query", "17"
                )),
                CONTEXT
        );

        assertThat(result.content()).isEqualTo("ok");
        verify(strategy).execute("17", CONTEXT);
    }

    @Test
    void rejectsUnsupportedLookupMode() {
        var strategy = mock(EditorialContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        when(strategy.supports("ALBUM_NAME")).thenReturn(false);
        var tool = new EditorialContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.EDITORIAL_CONTEXT, Map.of(
                        "lookupMode", "ALBUM_NAME",
                        "query", "Kind of Blue"
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported EDITORIAL_CONTEXT lookupMode");
    }

    @Test
    void rejectsBlankLookupMode() {
        var strategy = mock(EditorialContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new EditorialContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.EDITORIAL_CONTEXT, Map.of(
                        "lookupMode", "  ",
                        "query", "17"
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires non-blank lookupMode");
    }

    @Test
    void rejectsBlankQuery() {
        var strategy = mock(EditorialContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new EditorialContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.EDITORIAL_CONTEXT, Map.of(
                        "lookupMode", "LOG_NUMBER",
                        "query", "   "
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires non-blank query");
    }
}
