package com.marcoromanofinaa.jazzlogs.ai.recommend.core.retrieval;

import com.marcoromanofinaa.jazzlogs.core.exception.VectorStoreNotConfiguredException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class RecommendRetrievalService {

    private static final String ALBUM_FILTER = "type == 'ALBUM_LOG'";
    private static final String TRACK_FILTER = "type == 'TRACK_NOTE'";

    private final Optional<VectorStore> vectorStore;

    public List<Document> topAlbumDocuments(String query, int limit) {
        return similaritySearch(query, limit, ALBUM_FILTER);
    }

    public List<Document> topTrackDocuments(String query, int limit) {
        return similaritySearch(query, limit, TRACK_FILTER);
    }

    private List<Document> similaritySearch(String query, int limit, String filterExpression) {
        Assert.hasText(query, "Query must not be blank");
        Assert.isTrue(limit > 0, "Limit must be greater than zero");

        var request = SearchRequest.builder()
                .query(query)
                .topK(limit)
                .filterExpression(filterExpression)
                .build();

        return vectorStore()
                .similaritySearch(request);
    }

    private VectorStore vectorStore() {
        return vectorStore.orElseThrow(VectorStoreNotConfiguredException::new);
    }
}
