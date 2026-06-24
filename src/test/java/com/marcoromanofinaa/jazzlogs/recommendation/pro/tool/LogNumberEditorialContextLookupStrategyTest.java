package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.Neo4jClient;

class LogNumberEditorialContextLookupStrategyTest {

    private static final JazzAgentContext CONTEXT =
            new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null);

    @Test
    void exposesSupportedLookupMode() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var strategy = new LogNumberEditorialContextLookupStrategy(neo4jClient);

        assertThat(strategy.lookupMode()).isEqualTo("LOG_NUMBER");
        assertThat(strategy.supports("LOG_NUMBER")).isTrue();
        assertThat(strategy.supports("ALBUM_NAME")).isFalse();
    }

    @Test
    void returnsEditorialContextForExistingLogNumber() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())
                .bind(anyInt())
                .to(anyString())
                .fetch()
                .one()).thenReturn(java.util.Optional.of(Map.of(
                        "id", "album-node-1",
                        "logNumber", 17,
                        "album", "Kind of Blue",
                        "mainArtists", List.of("Miles Davis"),
                        "captionEssence", "Modal calm, midnight air.",
                        "albumContext", "A canonical doorway into modal jazz.",
                        "whyItMatters", "It reframed space and restraint.",
                        "editorialNote", "Recommended as a foundational late-night listen.",
                        "instagramPermalink", "https://instagram.com/p/example"
                )));

        var strategy = new LogNumberEditorialContextLookupStrategy(neo4jClient);

        var result = strategy.execute("17", CONTEXT);

        assertThat(result.toolName()).isEqualTo(JazzToolName.EDITORIAL_CONTEXT);
        assertThat(result.content()).contains("Use metadata as the source of truth");
        assertThat(result.metadata()).containsEntry("found", true);
        assertThat(result.metadata()).containsEntry("resolvedCatalogItem", true);
        assertThat(result.metadata()).containsEntry("recommendationType", "ALBUM");
        assertThat(result.metadata()).containsEntry("id", "album-node-1");
        assertThat(result.metadata()).containsEntry("winnerName", "Kind of Blue");
        assertThat(result.metadata()).containsEntry("artistFullName", "Miles Davis");
        assertThat(result.metadata()).containsEntry("logNumber", 17);
        assertThat(result.metadata()).containsEntry("album", "Kind of Blue");
    }

    @Test
    void returnsNotFoundPayloadWhenLogDoesNotExist() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString())
                .bind(anyInt())
                .to(anyString())
                .fetch()
                .one()).thenReturn(java.util.Optional.empty());

        var strategy = new LogNumberEditorialContextLookupStrategy(neo4jClient);

        var result = strategy.execute("17", CONTEXT);

        assertThat(result.toolName()).isEqualTo(JazzToolName.EDITORIAL_CONTEXT);
        assertThat(result.content()).isEqualTo("No editorial context found for log number 17.");
        assertThat(result.metadata()).containsEntry("found", false);
        assertThat(result.metadata()).containsEntry("lookupMode", "LOG_NUMBER");
        assertThat(result.metadata()).containsEntry("query", "17");
        assertThat(result.metadata()).containsEntry("logNumber", 17);
    }

    @Test
    void rejectsNonNumericQuery() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var strategy = new LogNumberEditorialContextLookupStrategy(neo4jClient);

        assertThatThrownBy(() -> strategy.execute("kind of blue", CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be numeric");
    }
}
