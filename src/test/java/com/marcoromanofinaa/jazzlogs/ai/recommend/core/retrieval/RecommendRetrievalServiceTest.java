package com.marcoromanofinaa.jazzlogs.ai.recommend.core.retrieval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.core.exception.VectorStoreNotConfiguredException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

@ExtendWith(MockitoExtension.class)
class RecommendRetrievalServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    void retrievesTopAlbumDocumentsWithAlbumFilter() {
        var service = new RecommendRetrievalService(Optional.of(vectorStore));
        var expectedDocuments = List.of(Document.builder().id("1").text("album doc").build());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocuments);

        var documents = service.topAlbumDocuments("warm and nocturnal jazz", 3);

        assertThat(documents).isEqualTo(expectedDocuments);

        var requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        var request = requestCaptor.getValue();

        assertThat(request.getQuery()).isEqualTo("warm and nocturnal jazz");
        assertThat(request.getTopK()).isEqualTo(3);
        assertThat(request.getFilterExpression()).isEqualTo(
                SearchRequest.builder().filterExpression("type == 'ALBUM_LOG'").build().getFilterExpression()
        );
    }

    @Test
    void retrievesTopTrackDocumentsWithTrackFilter() {
        var service = new RecommendRetrievalService(Optional.of(vectorStore));
        var expectedDocuments = List.of(Document.builder().id("2").text("track doc").build());
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expectedDocuments);

        var documents = service.topTrackDocuments("three tracks for a late drive", 5);

        assertThat(documents).isEqualTo(expectedDocuments);

        var requestCaptor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(requestCaptor.capture());
        var request = requestCaptor.getValue();

        assertThat(request.getQuery()).isEqualTo("three tracks for a late drive");
        assertThat(request.getTopK()).isEqualTo(5);
        assertThat(request.getFilterExpression()).isEqualTo(
                SearchRequest.builder().filterExpression("type == 'TRACK_NOTE'").build().getFilterExpression()
        );
    }

    @Test
    void throwsWhenVectorStoreIsNotConfigured() {
        var service = new RecommendRetrievalService(Optional.empty());

        assertThatThrownBy(() -> service.topAlbumDocuments("anything", 3))
                .isInstanceOf(VectorStoreNotConfiguredException.class);
    }
}
