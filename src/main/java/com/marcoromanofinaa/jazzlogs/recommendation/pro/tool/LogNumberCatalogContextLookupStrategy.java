package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
public class LogNumberCatalogContextLookupStrategy extends AbstractCatalogContextLookupStrategy {

    static final String LOOKUP_MODE = "LOG_NUMBER";

    private final Neo4jClient neo4jClient;

    public LogNumberCatalogContextLookupStrategy(Neo4jClient neo4jClient) {
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
        return result.<JazzToolExecutionResult>map(resolved -> foundAlbumResult(
                        resolved.id(),
                        resolved.logNumber(),
                        resolved.album(),
                        resolved.mainArtists(),
                        resolved.captionEssence(),
                        resolved.albumContext(),
                        resolved.whyItMatters(),
                        resolved.editorialNote(),
                        resolved.instagramPermalink()
                ))
                .orElseGet(() -> notFoundResult(query, Map.of("logNumber", logNumber)));
    }

    private Optional<AlbumContextRow> findAlbumByLogNumber(int logNumber) {
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
        return row.map(this::toAlbumContextRow);
    }

    private AlbumContextRow toAlbumContextRow(Map<String, Object> row) {
        return new AlbumContextRow(
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
            throw new IllegalArgumentException("CATALOG_CONTEXT LOG_NUMBER query must be numeric: " + query, exception);
        }
    }

    private record AlbumContextRow(
            String id,
            Integer logNumber,
            String album,
            java.util.List<String> mainArtists,
            String captionEssence,
            String albumContext,
            String whyItMatters,
            String editorialNote,
            String instagramPermalink
    ) {
    }
}
