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

class CatalogContextLookupStrategiesTest {

    private static final JazzAgentContext CONTEXT =
            new JazzAgentContext(null, null, "msg", "America/Argentina/Buenos_Aires", null, List.of(), null);

    @Test
    void logNumberStrategyReturnsAlbumContext() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString()).bind(anyInt()).to(anyString()).fetch().one())
                .thenReturn(java.util.Optional.of(Map.of(
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

        var strategy = new LogNumberCatalogContextLookupStrategy(neo4jClient);
        var result = strategy.execute("17", CONTEXT);

        assertThat(result.toolName()).isEqualTo(JazzToolName.CATALOG_CONTEXT);
        assertThat(result.metadata()).containsEntry("recommendationType", "ALBUM");
        assertThat(result.metadata()).containsEntry("id", "album-node-1");
        assertThat(result.metadata()).containsEntry("winnerName", "Kind of Blue");
    }

    @Test
    void albumIdStrategyReturnsAlbumContext() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString()).bind(anyString()).to(anyString()).fetch().one())
                .thenReturn(java.util.Optional.of(Map.of(
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

        var strategy = new AlbumIdCatalogContextLookupStrategy(neo4jClient);
        var result = strategy.execute("album-node-1", CONTEXT);

        assertThat(result.toolName()).isEqualTo(JazzToolName.CATALOG_CONTEXT);
        assertThat(result.metadata()).containsEntry("recommendationType", "ALBUM");
        assertThat(result.metadata()).containsEntry("id", "album-node-1");
    }

    @Test
    void trackIdStrategyReturnsTrackContext() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        when(neo4jClient.query(anyString()).bind(anyString()).to(anyString()).fetch().one())
                .thenReturn(java.util.Optional.of(Map.of(
                        "id", "track-node-1",
                        "logNumber", 17,
                        "album", "Kind of Blue",
                        "track", "So What",
                        "mainArtists", List.of("Miles Davis", "John Coltrane"),
                        "editorialNote", "Open and iconic.",
                        "whyItHits", "The motif lands instantly.",
                        "bestMoment", "That first entrance after the bass vamp.",
                        "instagramPermalink", "https://instagram.com/p/example"
                )));

        var strategy = new TrackIdCatalogContextLookupStrategy(neo4jClient);
        var result = strategy.execute("track-node-1", CONTEXT);

        assertThat(result.toolName()).isEqualTo(JazzToolName.CATALOG_CONTEXT);
        assertThat(result.metadata()).containsEntry("recommendationType", "TRACKS");
        assertThat(result.metadata()).containsEntry("id", "track-node-1");
        assertThat(result.metadata()).containsEntry("winnerName", "So What");
    }

    @Test
    void logNumberStrategyRejectsNonNumericQuery() {
        var neo4jClient = mock(Neo4jClient.class, RETURNS_DEEP_STUBS);
        var strategy = new LogNumberCatalogContextLookupStrategy(neo4jClient);

        assertThatThrownBy(() -> strategy.execute("kind of blue", CONTEXT))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be numeric");
    }
}
