package com.marcoromanofinaa.jazzlogs.admin.editorial.embedding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EditorialEmbeddingSyncService {

    private final Neo4jClient neo4jClient;
    private final EmbeddingModel embeddingModel;

    public void syncAlbumEmbedding(String albumNodeId) {
        var row = neo4jClient.query("""
                MATCH (album:Album {id: $albumNodeId})
                OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                WITH album, collect(DISTINCT artist.name) AS leaderArtists
                OPTIONAL MATCH (album)-[:BELONGS_TO]->(style:Style)
                WITH album, leaderArtists, collect(DISTINCT style.name) AS styles
                OPTIONAL MATCH (album)-[:EVOKES_MOOD]->(mood:Mood)
                WITH album, leaderArtists, styles, collect(DISTINCT mood.name) AS moods
                OPTIONAL MATCH (album)-[:PERFECT_FOR]->(context:Context)
                RETURN album.id AS id,
                       album.name AS name,
                       album.releaseDate AS releaseDate,
                       album.albumContext AS albumContext,
                       album.captionEssence AS captionEssence,
                       album.whyItMatters AS whyItMatters,
                       album.editorialNote AS editorialNote,
                       album.energy AS energy,
                       album.accessibility AS accessibility,
                       album.vocalProfile AS vocalProfile,
                       album.tier AS tier,
                       album.recommendedIf AS recommendedIf,
                       album.avoidIf AS avoidIf,
                       leaderArtists,
                       styles,
                       moods,
                       collect(DISTINCT context.name) AS contexts
                LIMIT 1
                """)
                .bind(albumNodeId).to("albumNodeId")
                .fetch()
                .one()
                .orElseThrow(() -> new IllegalStateException("Album graph node not found: " + albumNodeId));

        var text = joinParagraphs(
                sentence(
                        "%s is an album led by %s, released in %s.",
                        string(row, "name"),
                        joinNames(row.get("leaderArtists")),
                        string(row, "releaseDate")
                ),
                sentence(
                        "It sits around %s, with moods like %s.",
                        joinNames(row.get("styles")),
                        joinNames(row.get("moods"))
                ),
                sentence(
                        "It works especially well in contexts like %s, with %s energy, %s accessibility, and a %s vocal profile.",
                        joinNames(row.get("contexts")),
                        string(row, "energy"),
                        string(row, "accessibility"),
                        string(row, "vocalProfile")
                ),
                sentence("It belongs to the %s tier.", string(row, "tier")),
                sentence("%s", string(row, "albumContext")),
                sentence("%s", string(row, "captionEssence")),
                sentence("%s", string(row, "whyItMatters")),
                sentence("%s", string(row, "editorialNote")),
                sentence("Recommended if %s.", string(row, "recommendedIf")),
                sentence("Avoid if %s.", string(row, "avoidIf"))
        );

        updateEmbedding("Album", albumNodeId, text, "Failed to sync album embedding for graph node " + albumNodeId);
    }

    public void syncTrackEmbedding(String trackNodeId) {
        var row = neo4jClient.query("""
                MATCH (track:Track {id: $trackNodeId})
                OPTIONAL MATCH (album:Album)-[contains:CONTAINS]->(track)
                WITH track, album, contains
                OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                WITH track, album, contains, artist, performance
                ORDER BY coalesce(performance.primaryCredit, false) DESC, coalesce(performance.position, 999) ASC, artist.name ASC
                WITH track, album, contains, collect(DISTINCT artist.name) AS artistNames
                OPTIONAL MATCH (track)-[:EVOKES_MOOD]->(mood:Mood)
                WITH track, album, contains, artistNames, collect(DISTINCT mood.name) AS moods
                OPTIONAL MATCH (track)-[:PERFECT_FOR]->(context:Context)
                WITH track, album, contains, artistNames, moods, collect(DISTINCT context.name) AS contexts
                OPTIONAL MATCH (track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                WITH track, album, contains, artistNames, moods, contexts, collect(DISTINCT instrument.name) AS instruments
                OPTIONAL MATCH (track)-[:HAS_RHYTHM]->(rhythm:Rhythm)
                RETURN track.id AS id,
                       track.name AS name,
                       track.bestMoment AS bestMoment,
                       track.whyItHits AS whyItHits,
                       track.editorialNote AS editorialNote,
                       track.energy AS energy,
                       track.accessibility AS accessibility,
                       track.tempoFeel AS tempoFeel,
                       track.vocalProfile AS vocalProfile,
                       track.vocalStyle AS vocalStyle,
                       track.tier AS tier,
                       track.recommendedIf AS recommendedIf,
                       track.avoidIf AS avoidIf,
                       track.compositionType AS compositionType,
                       toString(track.isStandout) AS standout,
                       album.name AS albumName,
                       contains.trackRole AS trackRole,
                       instruments,
                       collect(DISTINCT rhythm.name) AS rhythms,
                       artistNames,
                       moods,
                       contexts
                LIMIT 1
                """)
                .bind(trackNodeId).to("trackNodeId")
                .fetch()
                .one()
                .orElseThrow(() -> new IllegalStateException("Track graph node not found: " + trackNodeId));

        var text = joinParagraphs(
                sentence(
                        "%s is a track from %s by %s.",
                        string(row, "name"),
                        string(row, "albumName"),
                        joinNames(row.get("artistNames"))
                ),
                sentence(
                        "It plays the role of %s on the album and leans toward %s.",
                        string(row, "trackRole"),
                        string(row, "compositionType")
                ),
                sentence(
                        "Its feel is shaped by moods like %s, contexts like %s, instruments such as %s, and rhythms like %s.",
                        joinNames(row.get("moods")),
                        joinNames(row.get("contexts")),
                        joinNames(row.get("instruments")),
                        joinNames(row.get("rhythms"))
                ),
                sentence(
                        "It has %s energy, %s accessibility, a %s tempo feel, and a %s vocal profile with %s vocal style.",
                        string(row, "energy"),
                        string(row, "accessibility"),
                        string(row, "tempoFeel"),
                        string(row, "vocalProfile"),
                        string(row, "vocalStyle")
                ),
                sentence("It is marked as standout %s and belongs to the %s tier.", string(row, "standout"), string(row, "tier")),
                sentence("A key moment in the track is %s.", string(row, "bestMoment")),
                sentence("%s", string(row, "whyItHits")),
                sentence("%s", string(row, "editorialNote")),
                sentence("Recommended if %s.", string(row, "recommendedIf")),
                sentence("Avoid if %s.", string(row, "avoidIf"))
        );

        updateEmbedding("Track", trackNodeId, text, "Failed to sync track embedding for graph node " + trackNodeId);
    }

    public void syncArtistEmbedding(String artistNodeId) {
        var row = neo4jClient.query("""
                MATCH (artist:Artist {id: $artistNodeId})
                OPTIONAL MATCH (artist)-[:PLAYS_INSTRUMENT]->(primaryInstrument:Instrument)
                WITH artist, primaryInstrument
                OPTIONAL MATCH (artist)-[:HAS_STYLE]->(style:Style)
                WITH artist, primaryInstrument, collect(DISTINCT style.name) AS styles
                OPTIONAL MATCH (artist)-[:PERFECT_FOR]->(context:Context)
                WITH artist, primaryInstrument, styles, collect(DISTINCT context.name) AS contexts
                OPTIONAL MATCH (artist)-[:SIMILAR_TO]->(related:Artist)
                WITH artist, primaryInstrument, styles, contexts, collect(DISTINCT related.name) AS relatedArtists
                OPTIONAL MATCH (artist)-[:LEADER_OF]->(album:Album)
                RETURN artist.id AS id,
                       artist.name AS name,
                       artist.signatureSound AS signatureSound,
                       artist.artistContext AS artistContext,
                       artist['editorialImportance'] AS editorialImportance,
                       artist.jazzlogsTake AS jazzlogsTake,
                       artist.avoidIf AS avoidIf,
                       artist.logAppearances AS logAppearances,
                       primaryInstrument.name AS primaryInstrument,
                       styles,
                       contexts,
                       relatedArtists,
                       collect(DISTINCT album.name) AS albums
                LIMIT 1
                """)
                .bind(artistNodeId).to("artistNodeId")
                .fetch()
                .one()
                .orElseThrow(() -> new IllegalStateException("Artist graph node not found: " + artistNodeId));

        var text = joinParagraphs(
                sentence(
                        "%s is an artist associated with %s, primarily connected to %s.",
                        string(row, "name"),
                        joinNames(row.get("styles")),
                        string(row, "primaryInstrument")
                ),
                sentence(
                        "Their music fits especially well in contexts like %s and relates to artists such as %s.",
                        joinNames(row.get("contexts")),
                        joinNames(row.get("relatedArtists"))
                ),
                sentence("Albums in this graph include %s.", joinNames(row.get("albums"))),
                sentence("%s", string(row, "signatureSound")),
                sentence("%s", string(row, "artistContext")),
                sentence("%s", string(row, "editorialImportance")),
                sentence("%s", string(row, "jazzlogsTake")),
                sentence("Avoid if %s.", string(row, "avoidIf"))
        );

        updateEmbedding("Artist", artistNodeId, text, "Failed to sync artist embedding for graph node " + artistNodeId);
    }

    private void updateEmbedding(String label, String nodeId, String text, String errorMessage) {
        try {
            var embedding = toDoubleList(embeddingModel.embed(text));
            neo4jClient.query("""
                    MATCH (node:%s {id: $nodeId})
                    SET node.embedding = $embedding
                    """.formatted(label))
                    .bind(nodeId).to("nodeId")
                    .bind(embedding).to("embedding")
                    .run();
        } catch (RuntimeException exception) {
            throw new EditorialEmbeddingSyncException(errorMessage, exception);
        }
    }

    private List<Double> toDoubleList(float[] embedding) {
        var values = new ArrayList<Double>(embedding.length);
        for (float value : embedding) {
            values.add((double) value);
        }
        return List.copyOf(values);
    }

    private String sentence(String template, Object... args) {
        for (var arg : args) {
            if (arg == null) {
                return null;
            }
            if (arg instanceof String value && value.isBlank()) {
                return null;
            }
        }
        return template.formatted(args);
    }

    private String joinParagraphs(String... lines) {
        var parts = new ArrayList<String>();
        for (var line : lines) {
            if (line != null && !line.isBlank()) {
                parts.add(line.trim());
            }
        }
        return String.join(". ", parts);
    }

    private String string(Map<String, Object> row, String key) {
        var value = row.get(key);
        return value == null ? null : String.valueOf(value);
    }


    private String joinNames(Object rawValues) {
        if (!(rawValues instanceof List<?> values) || values.isEmpty()) {
            return null;
        }
        var parts = new ArrayList<String>();
        for (var value : values) {
            if (value != null) {
                var rendered = String.valueOf(value).trim();
                if (!rendered.isBlank()) {
                    parts.add(rendered);
                }
            }
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }
}
