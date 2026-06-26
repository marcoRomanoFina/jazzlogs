package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.neo4j.core.Neo4jClient;

class ResolveJazzlogsEntityToolTest {

    private static final JazzAgentContext CONTEXT =
            new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null);

    @Test
    void exposesDeclaredNameAndSchema() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var tool = new ResolveJazzlogsEntityTool(neo4jClient);

        assertThat(tool.name()).isEqualTo(JazzToolName.RESOLVE_JAZZLOGS_ENTITY);
        assertThat(tool.description()).contains("bounded fuzzy fallback");
        assertThat(tool.parametersSchema()).containsKey("properties");
    }

    @Test
    void resolvesAlbumCandidatesWithFuzzyRanking() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        mockAlbumBucketQuery(neo4jClient, "STARTS WITH", List.of(
                Map.of(
                        "id", "album-1",
                        "name", "Kind of Blue",
                        "normalizedName", "kind of blue",
                        "mainArtists", List.of("Miles Davis")
                )
        ));
        mockAlbumBucketQuery(neo4jClient, "CONTAINS", List.of(
                Map.of(
                        "id", "album-2",
                        "name", "Blue Hour",
                        "normalizedName", "blue hour",
                        "mainArtists", List.of("Stanley Turrentine")
                )
        ));
        mockAlbumBucketQuery(neo4jClient, " = $normalizedQuery", List.of());
        mockAlbumBucketQuery(neo4jClient, "any(token IN $queryTokens", List.of());

        var tool = new ResolveJazzlogsEntityTool(neo4jClient);
        var result = tool.execute(
                new JazzToolCall(JazzToolName.RESOLVE_JAZZLOGS_ENTITY, Map.of(
                        "entityType", "ALBUM",
                        "query", "kind blue"
                )),
                CONTEXT
        );

        assertThat(result.toolName()).isEqualTo(JazzToolName.RESOLVE_JAZZLOGS_ENTITY);
        assertThat(result.metadata()).containsEntry("found", true);
        var candidates = (List<?>) result.metadata().get("candidates");
        assertThat(candidates).hasSize(2);
        assertThat(result.content()).contains("Resolved");
    }

    @Test
    void returnsNoCandidatesWhenNothingMatches() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString()).fetch().all()).thenReturn(List.of());

        var tool = new ResolveJazzlogsEntityTool(neo4jClient);
        var result = tool.execute(
                new JazzToolCall(JazzToolName.RESOLVE_JAZZLOGS_ENTITY, Map.of(
                        "entityType", "ARTIST",
                        "query", "zzzzzz"
                )),
                CONTEXT
        );

        assertThat(result.metadata()).containsEntry("found", false);
        assertThat((List<?>) result.metadata().get("candidates")).isEmpty();
    }

    @Test
    void rejectsUnsupportedEntityType() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var tool = new ResolveJazzlogsEntityTool(neo4jClient);

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.RESOLVE_JAZZLOGS_ENTITY, Map.of(
                        "entityType", "LABEL",
                        "query", "Blue Note"
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported RESOLVE_JAZZLOGS_ENTITY entityType");
    }

    @Test
    void rejectsBlankQuery() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var tool = new ResolveJazzlogsEntityTool(neo4jClient);

        assertThatThrownBy(() -> tool.execute(
                new JazzToolCall(JazzToolName.RESOLVE_JAZZLOGS_ENTITY, Map.of(
                        "entityType", "ALBUM",
                        "query", "  "
                )),
                CONTEXT
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("requires non-blank query");
    }

    private void mockAlbumBucketQuery(Neo4jClient neo4jClient, String queryFragment, List<Map<String, Object>> rows) {
        when(neo4jClient.query(contains(queryFragment))
                .bind(anyString()).to(anyString())
                .bind(anyList()).to(anyString())
                .bind(anyInt()).to(anyString())
                .fetch().all())
                .thenReturn(rows);
    }
}
