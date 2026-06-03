package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import com.marcoromanofinaa.jazzlogs.chat.session.RecommendedItemMetadataLookupService;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RetrievalServiceTest {

    @Test
    void retrieveRelevantDocumentsAppliesAnchorAndExcludeWinnerFilters() {
        var vectorStore = Mockito.mock(VectorStore.class);
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var retrievalService = new RetrievalService(vectorStore, resolver);

        when(resolver.findByWinners(BasicRecommendationTarget.ALBUM, List.of("Blue Hour")))
                .thenReturn(java.util.Map.of("blue hour", metadata("album-id-1")));
        when(resolver.findByWinners(BasicRecommendationTarget.ALBUM, List.of("Track A")))
                .thenReturn(java.util.Map.of("track a", metadata("track-id-1")));

        retrievalService.retrieveRelevantDocuments(new RetrievalCommand(
                "recommend something",
                BasicRecommendationTarget.ALBUM,
                3,
                List.of("Blue Hour"),
                List.of("Track A")
        ));

        var searchRequest = Mockito.mockingDetails(vectorStore)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("similaritySearch"))
                .map(invocation -> (SearchRequest) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(searchRequest.hasFilterExpression()).isTrue();
        var filterStr = searchRequest.getFilterExpression().toString();
        assertThat(filterStr).contains("album-id-1");
        assertThat(filterStr).contains("track-id-1");
    }

    @Test
    void retrieveRelevantDocumentsFiltersByAlbumSourceTypeForAlbumTarget() {
        var vectorStore = Mockito.mock(VectorStore.class);
        var retrievalService = new RetrievalService(
                vectorStore,
                Mockito.mock(RecommendedItemMetadataLookupService.class)
        );

        retrievalService.retrieveRelevantDocuments(new RetrievalCommand(
                "blue hour",
                BasicRecommendationTarget.ALBUM,
                3,
                List.of(),
                List.of()
        ));

        var searchRequest = Mockito.mockingDetails(vectorStore)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("similaritySearch"))
                .map(invocation -> (SearchRequest) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(searchRequest.hasFilterExpression()).isTrue();
        assertThat(searchRequest.getFilterExpression().toString()).contains("ALBUM_LOG");
    }

    @Test
    void retrieveRelevantDocumentsPushesSourceTypeSectionAndMetadataFiltersIntoSearchRequest() {
        var vectorStore = Mockito.mock(VectorStore.class);
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var retrievalService = new RetrievalService(vectorStore, resolver);

        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        when(resolver.findByWinners(BasicRecommendationTarget.ALBUM, List.of("Blue Hour")))
                .thenReturn(java.util.Map.of("blue hour", metadata("album-id-2")));

        retrievalService.retrieveRelevantDocuments(new RetrievalCommand(
                "miles",
                BasicRecommendationTarget.ALBUM,
                3,
                List.of(),
                List.of("Blue Hour")
        ));

        var searchRequest = Mockito.mockingDetails(vectorStore)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("similaritySearch"))
                .map(invocation -> (SearchRequest) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(searchRequest.hasFilterExpression()).isTrue();
        assertThat(searchRequest.getFilterExpression().toString())
                .contains("sourceType")
                .contains("sourceId")
                .contains("album-id-2");
    }

    @Test
    void retrieveRelevantDocumentsExpandsTopKToCompensateForPostFiltering() {
        var vectorStore = Mockito.mock(VectorStore.class);
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var retrievalService = new RetrievalService(vectorStore, resolver);

        when(resolver.findByWinners(BasicRecommendationTarget.ALBUM, List.of("Blue Hour", "Chet")))
                .thenReturn(Map.of(
                        "blue hour", metadata("album-id-1"),
                        "chet", metadata("album-id-2")
                ));

        retrievalService.retrieveRelevantDocuments(new RetrievalCommand(
                "night jazz",
                BasicRecommendationTarget.ALBUM,
                3,
                List.of(),
                List.of("Blue Hour", "Chet")
        ));

        var searchRequest = Mockito.mockingDetails(vectorStore)
                .getInvocations().stream()
                .filter(invocation -> invocation.getMethod().getName().equals("similaritySearch"))
                .map(invocation -> (SearchRequest) invocation.getArgument(0))
                .findFirst()
                .orElseThrow();

        assertThat(searchRequest.getTopK()).isGreaterThan(3);
    }

    @Test
    void retrieveRelevantDocumentsPostFiltersBySourceIdWhenMetadataIsResolved() {
        var vectorStore = Mockito.mock(VectorStore.class);
        var resolver = Mockito.mock(RecommendedItemMetadataLookupService.class);
        var retrievalService = new RetrievalService(vectorStore, resolver);

        when(resolver.findByWinners(BasicRecommendationTarget.TRACKS, List.of("Tenderly")))
                .thenReturn(Map.of("tenderly", trackMetadata("track-id-1")));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(
                new Document("Track one", Map.of("sourceType", "TRACK_LOG", "sourceId", "track-id-1", "track", "Tenderly")),
                new Document("Track two", Map.of("sourceType", "TRACK_LOG", "sourceId", "track-id-2", "track", "Tenderly"))
        ));

        var results = retrievalService.retrieveRelevantDocuments(new RetrievalCommand(
                "tenderly",
                BasicRecommendationTarget.TRACKS,
                5,
                List.of(),
                List.of("Tenderly")
        ));

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().getMetadata().get("sourceId")).isEqualTo("track-id-2");
    }

    private ChatRecommendationMemory.RecommendedItemMetadata metadata(String sourceId) {
        return new ChatRecommendationMemory.RecommendedItemMetadata(
                "ALBUM_LOG",
                sourceId,
                "Blue Hour",
                null,
                "Stanley Turrentine",
                "Stanley Turrentine",
                List.of(),
                "spotify-album",
                null,
                "A",
                "1961",
                "Hard Bop",
                "Instrumental",
                List.of(),
                List.of(),
                "medium",
                "high",
                null,
                null,
                null,
                null
        );
    }

    private ChatRecommendationMemory.RecommendedItemMetadata trackMetadata(String sourceId) {
        return new ChatRecommendationMemory.RecommendedItemMetadata(
                "TRACK_LOG",
                sourceId,
                "Some Album",
                "Tenderly",
                "Artist",
                "Artist",
                List.of(),
                "spotify-album",
                "spotify-track",
                "A",
                "1961",
                "Ballad",
                "Vocal",
                List.of(),
                List.of(),
                "low",
                "medium",
                null,
                null,
                null,
                null
        );
    }
}
