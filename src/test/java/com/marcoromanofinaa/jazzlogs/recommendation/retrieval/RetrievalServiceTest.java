package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReference;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReferenceType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.neo4j.core.Neo4jClient;

class RetrievalServiceTest {

    @Test
    void trackWhereClauseConstrainsResultsToReferencedAlbum() throws Exception {
        var service = new RetrievalService(
                Mockito.mock(Neo4jClient.class),
                Mockito.mock(EmbeddingModel.class),
                new RetrievalPhasePlanner()
        );

        var filters = new ConversationSubgraphFilters(
                List.of("Modal Jazz"),
                List.of(),
                List.of(),
                List.of(new ConversationSubgraphReference(
                        ConversationSubgraphReferenceType.ALBUM,
                        "Kind Of Blue"
                ))
        );
        var command = new RetrievalCommand(
                "temas de kind of blue",
                BasicRecommendationTarget.TRACKS,
                12,
                List.of(),
                filters
        );

        var method = RetrievalService.class.getDeclaredMethod("buildTrackWhereClause", RetrievalCommand.class);
        method.setAccessible(true);

        var whereClause = (String) method.invoke(service, command);

        assertThat(whereClause).contains("referenceAlbumRefs");
        assertThat(whereClause).contains("album.name");
        assertThat(whereClause).contains("leader.name");
    }

    @Test
    void trackQueryFiltersAfterOptionalAlbumMatchSoExcludedTracksAreRemoved() throws Exception {
        var service = new RetrievalService(
                Mockito.mock(Neo4jClient.class),
                Mockito.mock(EmbeddingModel.class),
                new RetrievalPhasePlanner()
        );

        var method = RetrievalService.class.getDeclaredMethod("buildTrackQuery", RetrievalCommand.class);
        method.setAccessible(true);

        var query = (String) method.invoke(service, new RetrievalCommand(
                "algo",
                BasicRecommendationTarget.TRACKS,
                12,
                List.of("track-node-1"),
                null
        ));

        assertThat(query).contains("OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)");
        assertThat(query).contains("WITH track, album, score");
        assertThat(query).contains("WHERE NOT track.id IN $excludedNodeIds");
    }

    @Test
    void candidateKeyUsesNodeIdInsteadOfTitleOnly() throws Exception {
        var service = new RetrievalService(
                Mockito.mock(Neo4jClient.class),
                Mockito.mock(EmbeddingModel.class),
                new RetrievalPhasePlanner()
        );

        var method = RetrievalService.class.getDeclaredMethod("candidateKey", com.marcoromanofinaa.jazzlogs.recommendation.basic.RecommendationCandidate.class);
        method.setAccessible(true);

        var first = new com.marcoromanofinaa.jazzlogs.recommendation.basic.RecommendationCandidate(
                "track-node-1",
                BasicRecommendationTarget.TRACKS,
                null,
                null,
                null,
                "Summertime",
                "Porgy and Bess",
                "Summertime",
                "Miles Davis",
                List.of(),
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
        var second = new com.marcoromanofinaa.jazzlogs.recommendation.basic.RecommendationCandidate(
                "track-node-2",
                BasicRecommendationTarget.TRACKS,
                null,
                null,
                null,
                "Summertime",
                "Summertime",
                "Summertime",
                "Ella Fitzgerald",
                List.of(),
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );

        var firstKey = (String) method.invoke(service, first);
        var secondKey = (String) method.invoke(service, second);

        assertThat(firstKey).isNotEqualTo(secondKey);
        assertThat(firstKey).contains("track-node-1");
        assertThat(secondKey).contains("track-node-2");
    }

    @Test
    void albumQueryProximityScoreUsesReferenceAlbumNameWithoutMissingArtistField() throws Exception {
        var service = new RetrievalService(
                Mockito.mock(Neo4jClient.class),
                Mockito.mock(EmbeddingModel.class),
                new RetrievalPhasePlanner()
        );

        var method = RetrievalService.class.getDeclaredMethod("buildAlbumQuery", RetrievalCommand.class);
        method.setAccessible(true);

        var query = (String) method.invoke(service, new RetrievalCommand(
                "algo de kind of blue",
                BasicRecommendationTarget.ALBUM,
                8,
                List.of(),
                new ConversationSubgraphFilters(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new ConversationSubgraphReference(
                                ConversationSubgraphReferenceType.ALBUM,
                                "Kind Of Blue"
                        ))
                )
        ));

        assertThat(query).contains("referenceAlbum.name");
        assertThat(query).doesNotContain("referenceAlbum.artistName");
    }

    @Test
    void trackQueryProximityScoreUsesReferenceTrackNameWithoutMissingArtistField() throws Exception {
        var service = new RetrievalService(
                Mockito.mock(Neo4jClient.class),
                Mockito.mock(EmbeddingModel.class),
                new RetrievalPhasePlanner()
        );

        var method = RetrievalService.class.getDeclaredMethod("buildTrackQuery", RetrievalCommand.class);
        method.setAccessible(true);

        var query = (String) method.invoke(service, new RetrievalCommand(
                "quiero summertime",
                BasicRecommendationTarget.TRACKS,
                12,
                List.of(),
                new ConversationSubgraphFilters(
                        List.of(),
                        List.of(),
                        List.of(),
                        List.of(new ConversationSubgraphReference(
                                ConversationSubgraphReferenceType.TRACK,
                                "Summertime"
                        ))
                )
        ));

        assertThat(query).contains("referenceTrack.name");
        assertThat(query).doesNotContain("referenceTrack.artistName");
    }
}
