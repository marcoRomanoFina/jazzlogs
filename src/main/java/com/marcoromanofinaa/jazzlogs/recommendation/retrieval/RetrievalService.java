package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.chat.session.RecommendedItemMetadataLookupService;
import com.marcoromanofinaa.jazzlogs.chat.session.ChatRecommendationMemory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RetrievalService {

    private static final String ALBUM_SOURCE_TYPE = "ALBUM_LOG";
    private static final String TRACK_SOURCE_TYPE = "TRACK_LOG";

    private final VectorStore vectorStore;
    private final RecommendedItemMetadataLookupService recommendedItemMetadataLookupService;

    public List<Document> retrieveRelevantDocuments(RetrievalCommand command) {
        int requestedTopK = command.topK() == null ? SearchRequest.DEFAULT_TOP_K : command.topK();
        var resolvedFilters = resolveFilters(command);
        var expandedTopK = expandedTopK(requestedTopK, resolvedFilters);

        var searchRequestBuilder = SearchRequest.builder()
                .query(command.userMessage())
                .topK(expandedTopK)
                .similarityThresholdAll();

        var filterExpression = buildPreFilterExpression(command, resolvedFilters);
        if (!filterExpression.isBlank()) {
            searchRequestBuilder.filterExpression(filterExpression);
        }

        var results = vectorStore.similaritySearch(searchRequestBuilder.build());
        return applyPostFilters(results, command, resolvedFilters, requestedTopK);
    }

    private String buildPreFilterExpression(RetrievalCommand command, ResolvedFilters resolvedFilters) {
        var clauses = new java.util.ArrayList<String>();

        // 1. Filtro estricto por Target (Album vs Track) inyectado al SQL
        String expectedSourceType = command.target() == BasicRecommendationTarget.ALBUM 
                ? ALBUM_SOURCE_TYPE 
                : TRACK_SOURCE_TYPE;
        clauses.add("sourceType == '" + expectedSourceType + "'");

        // 2. Anchors y Exclusiones: solo los valores seguros viajan al SQL.
        addAnchorClause(clauses, resolvedFilters.anchorSourceIds());
        addExclusionClause(clauses, resolvedFilters.excludedSourceIds());

        return String.join(" && ", clauses);
    }

    private void addExclusionClause(List<String> clauses, Set<String> excludedSourceIds) {
        if (excludedSourceIds.isEmpty()) return;

        var quotedValues = excludedSourceIds.stream()
                .filter(this::isSafeForFilterExpression)
                .map(this::quote)
                .toList();

        if (!quotedValues.isEmpty()) {
            var values = "[" + String.join(", ", quotedValues) + "]";
            clauses.add("sourceId NOT IN " + values);
        }
    }

    private void addAnchorClause(List<String> clauses, Set<String> anchorSourceIds) {
        if (anchorSourceIds.isEmpty()) return;

        var quotedValues = anchorSourceIds.stream()
                .filter(this::isSafeForFilterExpression)
                .map(this::quote)
                .toList();

        if (!quotedValues.isEmpty()) {
            var values = "[" + String.join(", ", quotedValues) + "]";
            clauses.add("sourceId IN " + values);
        }
    }

    private List<Document> applyPostFilters(
            List<Document> documents,
            RetrievalCommand command,
            ResolvedFilters resolvedFilters,
            int requestedTopK
    ) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        return documents.stream()
                .filter(document -> matchesAnchors(document, command, resolvedFilters))
                .filter(document -> !matchesExcludedWinners(document, command, resolvedFilters))
                .limit(requestedTopK)
                .toList();
    }

    private boolean matchesAnchors(
            Document document,
            RetrievalCommand command,
            ResolvedFilters resolvedFilters
    ) {
        if (!hasValues(command.anchorWinners())) {
            return true;
        }

        var candidateSourceId = sourceId(document);
        if (!resolvedFilters.anchorSourceIds().isEmpty()) {
            return candidateSourceId.isPresent()
                    && resolvedFilters.anchorSourceIds().contains(candidateSourceId.orElseThrow());
        }
        return true;
    }

    private boolean matchesExcludedWinners(
            Document document,
            RetrievalCommand command,
            ResolvedFilters resolvedFilters
    ) {
        if (!hasValues(command.excludedWinners())) {
            return false;
        }

        var candidateSourceId = sourceId(document);
        if (!resolvedFilters.excludedSourceIds().isEmpty()) {
            return candidateSourceId.isPresent()
                    && resolvedFilters.excludedSourceIds().contains(candidateSourceId.orElseThrow());
        }
        return false;
    }

    private boolean hasValues(List<String> values) {
        return values != null && values.stream().anyMatch(value -> value != null && !value.isBlank());
    }

    private ResolvedFilters resolveFilters(RetrievalCommand command) {
        var anchorMetadata = recommendedItemMetadataLookupService.findByWinners(
                command.target(),
                safeValues(command.anchorWinners())
        );
        var excludedMetadata = recommendedItemMetadataLookupService.findByWinners(
                command.target(),
                safeValues(command.excludedWinners())
        );
        return new ResolvedFilters(
                sourceIds(anchorMetadata),
                sourceIds(excludedMetadata)
        );
    }

    private int expandedTopK(int requestedTopK, ResolvedFilters resolvedFilters) {
        var filterSlack = resolvedFilters.anchorSourceIds().size() + resolvedFilters.excludedSourceIds().size();
        return Math.max(requestedTopK, requestedTopK + Math.max(4, filterSlack));
    }

    private Set<String> sourceIds(Map<String, ChatRecommendationMemory.RecommendedItemMetadata> metadataByWinner) {
        return metadataByWinner.values().stream()
                .map(ChatRecommendationMemory.RecommendedItemMetadata::sourceId)
                .filter(sourceId -> sourceId != null && !sourceId.isBlank())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private List<String> safeValues(List<String> values) {
        return values == null ? List.of() : values;
    }

    private Optional<String> sourceId(Document document) {
        var metadata = document.getMetadata();
        if (metadata == null || metadata.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadata.get("sourceId")).map(Object::toString);
    }

    private boolean isSafeForFilterExpression(String value) {
        return value.indexOf('?') < 0
                && value.indexOf('\'') < 0
                && value.indexOf('"') < 0;
    }

    private String quote(String value) {
        return "'" + value.replace("\\", "\\\\").replace("'", "\\'") + "'";
    }

    private record ResolvedFilters(
            Set<String> anchorSourceIds,
            Set<String> excludedSourceIds
    ) {
    }
}
