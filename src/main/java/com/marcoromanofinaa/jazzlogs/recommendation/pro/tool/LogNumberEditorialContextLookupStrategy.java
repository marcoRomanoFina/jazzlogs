package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
public class LogNumberEditorialContextLookupStrategy implements EditorialContextLookupStrategy {

    static final String LOOKUP_MODE = "LOG_NUMBER";

    private final Neo4jClient neo4jClient;

    public LogNumberEditorialContextLookupStrategy(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public String lookupMode() {
        return LOOKUP_MODE;
    }

    @Override
    public JazzToolExecutionResult execute(String query, JazzAgentContext context) {
        var logNumber = parseLogNumber(query);
        var result = findAlbumByLogNumber(logNumber);
        if (result.isEmpty()) {
            return new JazzToolExecutionResult(
                    JazzToolName.EDITORIAL_CONTEXT,
                    "No editorial context found for log number " + logNumber + ".",
                    Map.of(
                            "found", false,
                            "lookupMode", lookupMode(),
                            "query", query,
                            "logNumber", logNumber
                    )
            );
        }

        var metadata = result.orElseThrow();
        return new JazzToolExecutionResult(
                JazzToolName.EDITORIAL_CONTEXT,
                "Editorial context resolved successfully. Use metadata as the source of truth for factual and curatorial details.",
                metadata.toMetadataMap()
        );
    }

    private Optional<EditorialContextResult> findAlbumByLogNumber(int logNumber) {
        var row = neo4jClient.query("""
                MATCH (album:Album {logNumber: $logNumber})
                CALL (album) {
                  OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                  WITH DISTINCT artist.name AS artistName
                  WHERE artistName IS NOT NULL
                  ORDER BY artistName ASC
                  RETURN collect(artistName) AS mainArtists
                }
                RETURN album.id AS id,
                       album.logNumber AS logNumber,
                       album.name AS album,
                       mainArtists,
                       album.captionEssence AS captionEssence,
                       album.albumContext AS albumContext,
                       album.whyItMatters AS whyItMatters,
                       album.editorialNote AS editorialNote,
                       album.instagramPermalink AS instagramPermalink
                LIMIT 1
                """)
                .bind(logNumber).to("logNumber")
                .fetch()
                .one();

        return row.map(this::toResult);
    }

    private EditorialContextResult toResult(Map<String, Object> row) {
        return new EditorialContextResult(
                stringValue(row.get("id")),
                integerValue(row.get("logNumber")),
                stringValue(row.get("album")),
                stringList(row.get("mainArtists")),
                stringValue(row.get("captionEssence")),
                stringValue(row.get("albumContext")),
                stringValue(row.get("whyItMatters")),
                stringValue(row.get("editorialNote")),
                stringValue(row.get("instagramPermalink"))
        );
    }

    private int parseLogNumber(String query) {
        try {
            return Integer.parseInt(query.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("EDITORIAL_CONTEXT LOG_NUMBER query must be numeric: " + query, exception);
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    private Integer integerValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        var result = new java.util.ArrayList<String>();
        for (var item : iterable) {
            var rendered = stringValue(item);
            if (rendered != null && !rendered.isBlank()) {
                result.add(rendered);
            }
        }
        return List.copyOf(result);
    }

    private record EditorialContextResult(
            String id,
            Integer logNumber,
            String album,
            List<String> mainArtists,
            String captionEssence,
            String albumContext,
            String whyItMatters,
            String editorialNote,
            String instagramPermalink
    ) {
        Map<String, Object> toMetadataMap() {
            var metadata = new LinkedHashMap<String, Object>();
            metadata.put("found", true);
            metadata.put("resolvedCatalogItem", true);
            metadata.put("recommendationType", BasicRecommendationTarget.ALBUM.name());
            metadata.put("id", id == null ? "" : id);
            metadata.put("winnerName", album == null ? "" : album);
            metadata.put("artistFullName", String.join(", ", mainArtists == null ? List.of() : mainArtists));
            metadata.put("logNumber", logNumber);
            metadata.put("album", album == null ? "" : album);
            metadata.put("mainArtists", mainArtists == null ? List.of() : mainArtists);
            metadata.put("captionEssence", captionEssence);
            metadata.put("albumContext", albumContext);
            metadata.put("whyItMatters", whyItMatters);
            metadata.put("editorialNote", editorialNote);
            metadata.put("instagramPermalink", instagramPermalink);
            return Map.copyOf(metadata);
        }
    }
}
