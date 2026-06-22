package com.marcoromanofinaa.jazzlogs.recommendation.retrieval;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.RecommendationCandidate;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphFilters;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReference;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.router.model.ConversationSubgraphReferenceType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RetrievalService {

    private static final int DEFAULT_CANDIDATE_LIMIT = 18;
    private static final int VECTOR_OVERFETCH_LIMIT = 150;
    private static final String ALBUM_VECTOR_INDEX = "album_embeddings";
    private static final String TRACK_VECTOR_INDEX = "track_embeddings";

    private final Neo4jClient neo4jClient;
    private final EmbeddingModel embeddingModel;
    private final RetrievalPhasePlanner retrievalPhasePlanner;

    public List<RecommendationCandidate> retrieveCandidates(RetrievalCommand command) {
        int requestedTopK = command.topK() == null ? DEFAULT_CANDIDATE_LIMIT : command.topK();
        int candidateLimit = Math.max(requestedTopK + 6, DEFAULT_CANDIDATE_LIMIT);
        var phases = retrievalPhasePlanner.plan(command);
        var queryEmbedding = toDoubleList(embeddingModel.embed(command.userMessage()));
        var aggregatedResults = new LinkedHashMap<String, RecommendationCandidate>();

        for (var phase : phases) {
            var phaseCommand = new RetrievalCommand(
                    command.userMessage(),
                    command.target(),
                    candidateLimit,
                    command.excludedNodeIds(),
                    phase.subgraphFilters()
            );
            var phaseResults = executePhase(phaseCommand, queryEmbedding);
            phaseResults.forEach(candidate -> aggregatedResults.putIfAbsent(candidateKey(candidate), candidate));

            if (log.isDebugEnabled()) {
                log.debug(
                        "Retrieval phase '{}' produced {} candidate(s) for query '{}'",
                        phase.name(),
                        phaseResults.size(),
                        command.userMessage()
                );
            }

            if (!phaseResults.isEmpty()) {
                break;
            }
        }

        return aggregatedResults.values().stream()
                .limit(requestedTopK)
                .toList();
    }

    private List<RecommendationCandidate> executePhase(RetrievalCommand command, List<Double> queryEmbedding) {
        var cypher = command.target() == BasicRecommendationTarget.ALBUM
                ? buildAlbumQuery(command)
                : buildTrackQuery(command);
        var parameters = buildParameters(command, queryEmbedding);
        var rows = neo4jClient.query(cypher)
                .bindAll(parameters)
                .fetch()
                .all();
        return rows.stream()
                .map(row -> toCandidate(command.target(), row))
                .toList();
    }

    private String buildAlbumQuery(RetrievalCommand command) {
        var whereClause = buildAlbumWhereClause(command);
        return """
                CALL db.index.vector.queryNodes('%s', $vectorQueryLimit, $queryEmbedding) YIELD node, score
                WITH node AS album, score
                WHERE %s
                CALL (album) {
                  OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                  WITH artist.name AS artistName
                  WHERE artistName IS NOT NULL
                  ORDER BY artistName ASC
                  RETURN collect(artistName) AS leaderNames
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:BELONGS_TO]->(style:Style)
                  WITH style.name AS styleName
                  WHERE styleName IS NOT NULL
                  ORDER BY styleName ASC
                  RETURN collect(styleName) AS styles
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:EVOKES_MOOD]->(mood:Mood)
                  WITH DISTINCT mood.name AS moodName
                  WHERE moodName IS NOT NULL
                  ORDER BY moodName ASC
                  RETURN collect(moodName) AS moods
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:PERFECT_FOR]->(context:Context)
                  WITH DISTINCT context.name AS contextName
                  WHERE contextName IS NOT NULL
                  ORDER BY contextName ASC
                  RETURN collect(contextName) AS contexts
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:CONTAINS]->(:Track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                  WITH instrument.name AS instrumentName
                  WHERE instrumentName IS NOT NULL
                  ORDER BY instrumentName ASC
                  RETURN head(collect(instrumentName)) AS instrumentFocus
                }
                WITH album, score, leaderNames, styles, moods, contexts, instrumentFocus,
                     CASE
                       WHEN EXISTS {
                         UNWIND $referenceAlbumRefs AS referenceAlbum
                         WHERE toLower(trim(album.name)) = toLower(trim(referenceAlbum.name))
                       } THEN 1.0
                       ELSE 0.0
                     END AS proximityScore
                RETURN
                  album.id AS nodeId,
                  album.name AS title,
                  album.name AS album,
                  null AS track,
                  leaderNames,
                  album.logNumber AS logNumber,
                  album.spotifyAlbumId AS spotifyAlbumId,
                  null AS spotifyTrackId,
                  album.tier AS tier,
                  head(styles) AS style,
                  album.vocalProfile AS vocalProfile,
                  moods AS moods,
                  album.energy AS energy,
                  album.accessibility AS accessibility,
                  null AS tempoFeel,
                  instrumentFocus AS instrumentFocus,
                  contexts AS listeningContext,
                  album.captionEssence AS captionEssence,
                  album.editorialNote AS editorialNote,
                  album.albumContext AS albumContext,
                  album.whyItMatters AS whyItMatters,
                  score AS semanticScore,
                  proximityScore AS proximityScore,
                  ((score * 0.75) + (proximityScore * 0.25)) AS finalScore
                ORDER BY finalScore DESC, semanticScore DESC, title ASC
                LIMIT $candidateLimit
                """.formatted(ALBUM_VECTOR_INDEX, whereClause);
    }

    private String buildTrackQuery(RetrievalCommand command) {
        var whereClause = buildTrackWhereClause(command);
        return """
                CALL db.index.vector.queryNodes('%s', $vectorQueryLimit, $queryEmbedding) YIELD node, score
                WITH node AS track, score
                OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                WITH track, album, score
                WHERE %s
                CALL (track) {
                  OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                  WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                  WHERE artistName IS NOT NULL
                  ORDER BY position ASC, artistName ASC
                  RETURN collect(artistName) AS artistNames
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:BELONGS_TO]->(style:Style)
                  WITH style.name AS styleName
                  WHERE styleName IS NOT NULL
                  ORDER BY styleName ASC
                  RETURN collect(styleName) AS styles
                }
                CALL (album) {
                  OPTIONAL MATCH (album)-[:EVOKES_MOOD]->(mood:Mood)
                  WITH DISTINCT mood.name AS moodName
                  WHERE moodName IS NOT NULL
                  ORDER BY moodName ASC
                  RETURN collect(moodName) AS moods
                }
                CALL (track) {
                  OPTIONAL MATCH (track)-[:PERFECT_FOR]->(context:Context)
                  WITH DISTINCT context.name AS contextName
                  WHERE contextName IS NOT NULL
                  ORDER BY contextName ASC
                  RETURN collect(contextName) AS contexts
                }
                CALL (track) {
                  OPTIONAL MATCH (track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                  WITH DISTINCT instrument.name AS instrumentName
                  WHERE instrumentName IS NOT NULL
                  ORDER BY instrumentName ASC
                  RETURN collect(instrumentName) AS instruments
                }
                WITH track, album, score, artistNames, styles, moods, contexts, instruments,
                     CASE
                       WHEN EXISTS {
                         UNWIND $referenceTrackRefs AS referenceTrack
                         WHERE toLower(trim(track.name)) = toLower(trim(referenceTrack.name))
                       } THEN 1.0
                       ELSE 0.0
                     END AS proximityScore
                RETURN
                  track.id AS nodeId,
                  track.name AS title,
                  album.name AS album,
                  track.name AS track,
                  artistNames,
                  track.logNumber AS logNumber,
                  album.spotifyAlbumId AS spotifyAlbumId,
                  track.spotify_track_id AS spotifyTrackId,
                  track.tier AS tier,
                  head(styles) AS style,
                  track.vocalProfile AS vocalProfile,
                  moods AS moods,
                  track.energy AS energy,
                  track.accessibility AS accessibility,
                  track.tempoFeel AS tempoFeel,
                  head(instruments) AS instrumentFocus,
                  contexts AS listeningContext,
                  CASE
                    WHEN coalesce(track.isStandout, false) THEN 'yes'
                    ELSE null
                  END AS standout,
                  track.compositionType AS compositionType,
                  track.editorialNote AS editorialNote,
                  track.whyItHits AS whyItHits,
                  score AS semanticScore,
                  proximityScore AS proximityScore,
                  ((score * 0.75) + (proximityScore * 0.25)) AS finalScore
                ORDER BY finalScore DESC, semanticScore DESC, title ASC
                LIMIT $candidateLimit
                """.formatted(TRACK_VECTOR_INDEX, whereClause);
    }

    private String buildAlbumWhereClause(RetrievalCommand command) {
        var clauses = new ArrayList<String>();
        clauses.add(idExclusionClause("album.id", safeExactValues(command.excludedNodeIds())));

        var filters = command.subgraphFilters();
        if (filters != null) {
            if (!safeValues(filters.styles()).isEmpty()) {
                clauses.add("""
                        EXISTS {
                          MATCH (album)-[:BELONGS_TO]->(style:Style)
                          WHERE ANY(styleToken IN [token IN split(style.name, '/') | toLower(trim(token))] WHERE styleToken IN $styles)
                        }
                        """);
            }
            if (!safeValues(filters.instruments()).isEmpty()) {
                clauses.add("""
                        EXISTS {
                          MATCH (album)-[:CONTAINS]->(:Track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                          WHERE toLower(trim(instrument.name)) IN $instruments
                        }
                        """);
            }
            if (!safeValues(filters.rhythms()).isEmpty()) {
                clauses.add("""
                        EXISTS {
                          MATCH (album)-[:CONTAINS]->(:Track)-[:HAS_RHYTHM]->(rhythm:Rhythm)
                          WHERE toLower(trim(rhythm.name)) IN $rhythms
                        }
                        """);
            }
            if (hasReferences(filters.references(), ConversationSubgraphReferenceType.ARTIST)) {
                clauses.add("""
                        (
                          EXISTS {
                            MATCH (artist:Artist)-[:LEADER_OF]->(album)
                            WHERE toLower(trim(artist.name)) IN $referenceArtists
                          } OR EXISTS {
                            MATCH (artist:Artist)-[:SIDEMAN_ON]->(album)
                            WHERE toLower(trim(artist.name)) IN $referenceArtists
                          }
                        )
                        """);
            }
        }

        return joinClauses(clauses);
    }

    private String buildTrackWhereClause(RetrievalCommand command) {
        var clauses = new ArrayList<String>();
        clauses.add(idExclusionClause("track.id", safeExactValues(command.excludedNodeIds())));

        var filters = command.subgraphFilters();
        if (filters != null) {
            if (!safeValues(filters.styles()).isEmpty()) {
                clauses.add("""
                        album IS NOT NULL AND EXISTS {
                          MATCH (album)-[:BELONGS_TO]->(style:Style)
                          WHERE ANY(styleToken IN [token IN split(style.name, '/') | toLower(trim(token))] WHERE styleToken IN $styles)
                        }
                        """);
            }
            if (!safeValues(filters.instruments()).isEmpty()) {
                clauses.add("""
                        EXISTS {
                          MATCH (track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                          WHERE toLower(trim(instrument.name)) IN $instruments
                        }
                        """);
            }
            if (!safeValues(filters.rhythms()).isEmpty()) {
                clauses.add("""
                        EXISTS {
                          MATCH (track)-[:HAS_RHYTHM]->(rhythm:Rhythm)
                          WHERE toLower(trim(rhythm.name)) IN $rhythms
                        }
                        """);
            }
            if (hasReferences(filters.references(), ConversationSubgraphReferenceType.ARTIST)) {
                clauses.add("""
                        EXISTS {
                          MATCH (artist:Artist)-[:PERFORMED_ON]->(track)
                          WHERE toLower(trim(artist.name)) IN $referenceArtists
                        }
                        """);
            }
            if (hasReferences(filters.references(), ConversationSubgraphReferenceType.ALBUM)) {
                clauses.add("""
                        album IS NOT NULL AND EXISTS {
                          UNWIND $referenceAlbumRefs AS referenceAlbum
                          MATCH (leader:Artist)-[:LEADER_OF]->(album)
                          WHERE toLower(trim(album.name)) = toLower(trim(referenceAlbum.name))
                            AND (
                              referenceAlbum.artistName IS NULL
                              OR toLower(trim(leader.name)) = toLower(trim(referenceAlbum.artistName))
                            )
                        }
                        """);
            }
        }

        return joinClauses(clauses);
    }

    private String joinClauses(List<String> clauses) {
        if (clauses.isEmpty()) {
            return "true";
        }
        return String.join(" AND ", clauses);
    }

    private Map<String, Object> buildParameters(RetrievalCommand command, List<Double> queryEmbedding) {
        var params = new LinkedHashMap<String, Object>();
        params.put("candidateLimit", command.topK() == null ? DEFAULT_CANDIDATE_LIMIT : command.topK());
        params.put("vectorQueryLimit", Math.max(
                command.topK() == null ? DEFAULT_CANDIDATE_LIMIT : command.topK(),
                VECTOR_OVERFETCH_LIMIT
        ));
        params.put("queryEmbedding", queryEmbedding);
        params.put("excludedNodeIds", safeExactValues(command.excludedNodeIds()));

        var filters = command.subgraphFilters();
        params.put("styles", filters == null ? List.of() : safeValues(filters.styles()));
        params.put("instruments", filters == null ? List.of() : safeValues(filters.instruments()));
        params.put("rhythms", filters == null ? List.of() : safeValues(filters.rhythms()));
        params.put("referenceArtists", referenceNames(filters, ConversationSubgraphReferenceType.ARTIST));
        params.put("referenceAlbumRefs", referenceEntityRefs(filters, ConversationSubgraphReferenceType.ALBUM));
        params.put("referenceTrackRefs", referenceEntityRefs(filters, ConversationSubgraphReferenceType.TRACK));
        return params;
    }

    private RecommendationCandidate toCandidate(BasicRecommendationTarget target, Map<String, Object> row) {
        var artistNames = stringList(row.get("leaderNames"));
        if (artistNames.isEmpty()) {
            artistNames = stringList(row.get("artistNames"));
        }
        return new RecommendationCandidate(
                stringValue(row.get("nodeId")),
                target,
                integerValue(row.get("logNumber")),
                stringValue(row.get("spotifyAlbumId")),
                stringValue(row.get("spotifyTrackId")),
                stringValue(row.get("title")),
                stringValue(row.get("album")),
                stringValue(row.get("track")),
                artistNames.isEmpty() ? null : artistNames.getFirst(),
                artistNames.size() > 1 ? artistNames.subList(1, artistNames.size()) : List.of(),
                stringValue(row.get("tier")),
                stringValue(row.get("style")),
                stringValue(row.get("vocalProfile")),
                stringList(row.get("moods")),
                stringValue(row.get("energy")),
                stringValue(row.get("accessibility")),
                stringValue(row.get("tempoFeel")),
                stringValue(row.get("instrumentFocus")),
                stringList(row.get("listeningContext")),
                stringValue(row.get("standout")),
                null, // albumRole
                stringValue(row.get("compositionType")),
                stringValue(row.get("captionEssence")),
                stringValue(row.get("editorialNote")),
                editorialText(row)
        );
    }

    private String candidateKey(RecommendationCandidate candidate) {
        if (candidate.nodeId() != null && !candidate.nodeId().isBlank()) {
            return candidate.recommendationType() + "|nodeId|" + candidate.nodeId();
        }

        return candidate.recommendationType()
                + "|title|" + candidate.title()
                + "|album|" + candidate.album()
                + "|track|" + candidate.track()
                + "|artist|" + candidate.primaryArtist();
    }

    private String idExclusionClause(String fieldName, List<String> excludedNodeIds) {
        if (excludedNodeIds.isEmpty()) {
            return "true";
        }
        return "NOT " + fieldName + " IN $excludedNodeIds";
    }

    private boolean hasReferences(
            List<ConversationSubgraphReference> references,
            ConversationSubgraphReferenceType type
    ) {
        return references != null && references.stream().anyMatch(reference -> reference != null && reference.type() == type);
    }

    private List<String> referenceNames(
            ConversationSubgraphFilters filters,
            ConversationSubgraphReferenceType type
    ) {
        if (filters == null || filters.references() == null) {
            return List.of();
        }
        return filters.references().stream()
                .filter(ref -> ref != null && ref.type() == type && ref.name() != null)
                .map(ref -> normalize(ref.name()))
                .distinct()
                .toList();
    }

    private List<Map<String, Object>> referenceEntityRefs(
            ConversationSubgraphFilters filters,
            ConversationSubgraphReferenceType type
    ) {
        if (filters == null || filters.references() == null) {
            return List.of();
        }
        return filters.references().stream()
                .filter(ref -> ref != null && ref.type() == type)
                .<Map<String, Object>>map(ref -> {
                    var map = new java.util.HashMap<String, Object>();
                    map.put("name", ref.name());
                    return map;
                })
                .toList();
    }

    private List<String> safeValues(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .map(this::normalize)
                .distinct()
                .toList();
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Collection<?> collection)) {
            return List.of();
        }
        return collection.stream()
                .map(this::stringValue)
                .filter(item -> item != null && !item.isBlank())
                .toList();
    }

    private String editorialText(Map<String, Object> row) {
        var parts = new ArrayList<String>();
        appendIfPresent(parts, stringValue(row.get("albumContext")));
        appendIfPresent(parts, stringValue(row.get("captionEssence")));
        appendIfPresent(parts, stringValue(row.get("whyItMatters")));
        appendIfPresent(parts, stringValue(row.get("whyItHits")));
        appendIfPresent(parts, stringValue(row.get("editorialNote")));
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private void appendIfPresent(List<String> parts, String value) {
        if (value != null && !value.isBlank()) {
            parts.add(value);
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
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Integer.valueOf(value.toString().trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> safeExactValues(List<String> values) {
        return values == null ? List.of() : values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .distinct()
                .toList();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private List<Double> toDoubleList(float[] embedding) {
        var values = new ArrayList<Double>(embedding.length);
        for (float value : embedding) {
            values.add((double) value);
        }
        return List.copyOf(values);
    }

}
