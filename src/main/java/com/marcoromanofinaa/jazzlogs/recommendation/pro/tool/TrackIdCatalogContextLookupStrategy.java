package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.Optional;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
public class TrackIdCatalogContextLookupStrategy extends AbstractCatalogContextLookupStrategy {

    static final String LOOKUP_MODE = "TRACK_ID";

    private final Neo4jClient neo4jClient;

    public TrackIdCatalogContextLookupStrategy(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public String lookupMode() {
        return LOOKUP_MODE;
    }

    @Override
    public JazzToolExecutionResult execute(String query, JazzAgentContext context) {
        var trackId = requireNonBlankId(query, "CATALOG_CONTEXT TRACK_ID");
        var result = findTrackById(trackId);
        return result.<JazzToolExecutionResult>map(resolved -> foundTrackResult(
                        resolved.id(),
                        resolved.logNumber(),
                        resolved.album(),
                        resolved.track(),
                        resolved.mainArtists(),
                        resolved.editorialNote(),
                        resolved.whyItHits(),
                        resolved.bestMoment(),
                        resolved.instagramPermalink()
                ))
                .orElseGet(() -> notFoundResult(query, java.util.Map.of()));
    }

    private Optional<TrackContextRow> findTrackById(String trackId) {
        var row = neo4jClient.query("""
                MATCH (track:Track {id: $trackId})
                OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                CALL (track) {
                  OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                  WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                  WHERE artistName IS NOT NULL
                  ORDER BY position ASC, artistName ASC
                  RETURN collect(artistName) AS mainArtists
                }
                RETURN track.id AS id,
                       track.logNumber AS logNumber,
                       album.name AS album,
                       track.name AS track,
                       mainArtists,
                       track.editorialNote AS editorialNote,
                       track.whyItHits AS whyItHits,
                       track.bestMoment AS bestMoment,
                       album.instagramPermalink AS instagramPermalink
                LIMIT 1
                """)
                .bind(trackId).to("trackId")
                .fetch()
                .one();
        return row.map(this::toTrackContextRow);
    }

    private TrackContextRow toTrackContextRow(java.util.Map<String, Object> row) {
        return new TrackContextRow(
                stringValue(row.get("id")),
                integerValue(row.get("logNumber")),
                stringValue(row.get("album")),
                stringValue(row.get("track")),
                stringList(row.get("mainArtists")),
                stringValue(row.get("editorialNote")),
                stringValue(row.get("whyItHits")),
                stringValue(row.get("bestMoment")),
                stringValue(row.get("instagramPermalink"))
        );
    }

    private record TrackContextRow(
            String id,
            Integer logNumber,
            String album,
            String track,
            java.util.List<String> mainArtists,
            String editorialNote,
            String whyItHits,
            String bestMoment,
            String instagramPermalink
    ) {
    }
}
