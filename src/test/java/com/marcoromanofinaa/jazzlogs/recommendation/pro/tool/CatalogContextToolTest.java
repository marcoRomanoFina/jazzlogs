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

class CatalogContextToolTest {

    private static final JazzAgentContext CONTEXT =
            new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null);

    @Test
    void exposesDeclaredNameAndSchema() {
        var strategy = mock(CatalogContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new CatalogContextTool(List.of(strategy));

        assertThat(tool.name()).isEqualTo(JazzToolName.CATALOG_CONTEXT);
        assertThat(tool.description()).contains("trusted JazzLogs catalog context");
        assertThat(tool.parametersSchema()).containsKey("properties");
    }

    @Test
    void delegatesToMatchingStrategy() {
        var strategy = mock(CatalogContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        when(strategy.supports("LOG_NUMBER")).thenReturn(true);
        when(strategy.execute("17", CONTEXT)).thenReturn(new JazzToolExecutionResult(
                JazzToolName.CATALOG_CONTEXT,
                "ok",
                Map.of("found", true, "id", "album-node-1")
        ));

        var tool = new CatalogContextTool(List.of(strategy));
        var result = tool.execute(
                new JazzToolCall(JazzToolName.CATALOG_CONTEXT, Map.of(
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
        var strategy = mock(CatalogContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        when(strategy.supports("ALBUM_NAME")).thenReturn(false);
        var tool = new CatalogContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.CATALOG_CONTEXT, Map.of(
                        "lookupMode", "ALBUM_NAME",
                        "query", "Kind of Blue"
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported CATALOG_CONTEXT lookupMode");
    }

    @Test
    void rejectsBlankLookupMode() {
        var strategy = mock(CatalogContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new CatalogContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.CATALOG_CONTEXT, Map.of(
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
        var strategy = mock(CatalogContextLookupStrategy.class);
        when(strategy.lookupMode()).thenReturn("LOG_NUMBER");
        var tool = new CatalogContextTool(List.of(strategy));

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.CATALOG_CONTEXT, Map.of(
                        "lookupMode", "LOG_NUMBER",
                        "query", "   "
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires non-blank query");
    }
}
