package com.marcoromanofinaa.jazzlogs.chat.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.springframework.data.neo4j.core.Neo4jClient;

@MockitoSettings
class RecommendedItemEnrichmentServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Neo4jClient neo4jClient;

    @Test
    void enrichFailsWithControlledErrorWhenWinnerHasNoNodeId() {
        var service = new RecommendedItemEnrichmentService(neo4jClient);

        assertThatThrownBy(() -> service.enrich(
                BasicRecommendationTarget.ALBUM,
                List.of(new WinnerReference(BasicRecommendationTarget.ALBUM, "", "Blue Hour", "Stanley Turrentine"))
        ))
                .isInstanceOf(RecommendedItemEnrichmentException.class)
                .hasMessageContaining("missing a valid node id");
    }

    @Test
    void enrichFailsWithControlledErrorWhenMetadataIsMissingForWinner() {
        var service = new RecommendedItemEnrichmentService(neo4jClient);
        var winner = new WinnerReference(BasicRecommendationTarget.ALBUM, "album-node-1", "Blue Hour", "Stanley Turrentine");

        when(neo4jClient.query(org.mockito.ArgumentMatchers.anyString())
                .bind(org.mockito.ArgumentMatchers.any())
                .to(org.mockito.ArgumentMatchers.eq("winnerNodeIds"))
                .fetch()
                .all())
                .thenReturn(List.<Map<String, Object>>of());

        assertThatThrownBy(() -> service.enrich(BasicRecommendationTarget.ALBUM, List.of(winner)))
                .isInstanceOf(RecommendedItemEnrichmentException.class)
                .hasMessageContaining("album-node-1");
    }

    @Test
    void enrichFailsWhenWinnerTypeDoesNotMatchRecommendationType() {
        var service = new RecommendedItemEnrichmentService(neo4jClient);

        assertThatThrownBy(() -> service.enrich(
                BasicRecommendationTarget.ALBUM,
                List.of(new WinnerReference(
                        BasicRecommendationTarget.TRACKS,
                        "track-node-1",
                        "My Foolish Heart",
                        "Bill Evans"
                ))
        ))
                .isInstanceOf(RecommendedItemEnrichmentException.class)
                .hasMessageContaining("winner types do not match");
    }
}
