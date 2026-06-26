package com.marcoromanofinaa.jazzlogs.recommendation.pro.tool;

import com.marcoromanofinaa.jazzlogs.recommendation.pro.agent.JazzAgentContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringJoiner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
public class ResolveJazzlogsEntityTool implements JazzTool {

    private static final String ENTITY_TYPE_ALBUM = "ALBUM";
    private static final String ENTITY_TYPE_TRACK = "TRACK";
    private static final String ENTITY_TYPE_ARTIST = "ARTIST";
    private static final String MATCH_TYPE_EXACT = "EXACT";
    private static final String MATCH_TYPE_PREFIX = "PREFIX";
    private static final String MATCH_TYPE_CONTAINS = "CONTAINS";
    private static final String MATCH_TYPE_FUZZY = "FUZZY";
    private static final int RESULT_LIMIT = 5;
    private static final int FUZZY_SHORTLIST_LIMIT = 20;

    private final Neo4jClient neo4jClient;

    public ResolveJazzlogsEntityTool(Neo4jClient neo4jClient) {
        this.neo4jClient = neo4jClient;
    }

    @Override
    public JazzToolName name() {
        return JazzToolName.RESOLVE_JAZZLOGS_ENTITY;
    }

    @Override
    public String description() {
        return """
                Use this when the user names a JazzLogs album, track, or artist but you do not yet know the exact catalog id.
                It resolves a short list of lexical candidates with exact, prefix, contains, and bounded fuzzy fallback matching.
                Use its results before calling CATALOG_CONTEXT when you need a stable catalog id.
                """;
    }

    @Override
    public Map<String, Object> parametersSchema() {
        var properties = new LinkedHashMap<String, Object>();
        properties.put("entityType", Map.of(
                "type", "string",
                "enum", List.of(ENTITY_TYPE_ALBUM, ENTITY_TYPE_TRACK, ENTITY_TYPE_ARTIST)
        ));
        properties.put("query", Map.of("type", "string"));

        var schema = new LinkedHashMap<String, Object>();
        schema.put("type", "object");
        schema.put("additionalProperties", false);
        schema.put("properties", properties);
        schema.put("required", List.of("entityType", "query"));
        return schema;
    }

    @Override
    public JazzToolExecutionResult execute(JazzToolCall call, JazzAgentContext context) {
        var entityType = requireEnumLike(call.arguments(), "entityType");
        var query = requireNonBlank(call.arguments(), "query");
        var normalizedQuery = normalize(query);
        var queryTokens = normalizedTokens(normalizedQuery);

        var candidates = resolveCandidates(entityType, normalizedQuery, queryTokens);

        var metadata = new LinkedHashMap<String, Object>();
        metadata.put("found", !candidates.isEmpty());
        metadata.put("entityType", entityType);
        metadata.put("query", query);
        metadata.put("candidates", candidates.stream().map(ResolvedCandidate::toMetadataMap).toList());

        return new JazzToolExecutionResult(
                name(),
                renderContent(entityType, query, candidates),
                Map.copyOf(metadata)
        );
    }

    private List<ResolvedCandidate> resolveCandidates(String entityType, String normalizedQuery, List<String> queryTokens) {
        var ordered = new LinkedHashMap<String, ResolvedCandidate>();
        appendMatches(ordered, findCandidates(entityType, normalizedQuery, queryTokens, MATCH_TYPE_EXACT, RESULT_LIMIT));
        appendMatches(ordered, findCandidates(entityType, normalizedQuery, queryTokens, MATCH_TYPE_PREFIX, remaining(ordered)));
        appendMatches(ordered, findCandidates(entityType, normalizedQuery, queryTokens, MATCH_TYPE_CONTAINS, remaining(ordered)));
        appendMatches(ordered, findFuzzyFallbackCandidates(entityType, normalizedQuery, queryTokens, remaining(ordered)));
        return ordered.values().stream().limit(RESULT_LIMIT).toList();
    }

    private int remaining(Map<String, ResolvedCandidate> resolved) {
        return Math.max(0, RESULT_LIMIT - resolved.size());
    }

    private void appendMatches(Map<String, ResolvedCandidate> ordered, List<ResolvedCandidate> candidates) {
        if (candidates == null || candidates.isEmpty() || ordered.size() >= RESULT_LIMIT) {
            return;
        }
        for (var candidate : candidates) {
            if (candidate == null || candidate.id() == null) {
                continue;
            }
            ordered.putIfAbsent(candidate.id(), candidate);
            if (ordered.size() >= RESULT_LIMIT) {
                return;
            }
        }
    }

    private List<ResolvedCandidate> findCandidates(
            String entityType,
            String normalizedQuery,
            List<String> queryTokens,
            String matchType,
            int limit
    ) {
        if (limit <= 0) {
            return List.of();
        }
        var rows = neo4jClient.query(queryFor(entityType, matchType))
                .bind(normalizedQuery).to("normalizedQuery")
                .bind(queryTokens).to("queryTokens")
                .bind(limit).to("limit")
                .fetch()
                .all();

        return rows.stream()
                .map(row -> toResolvedCandidate(entityType, matchType, row))
                .filter(Objects::nonNull)
                .toList();
    }

    private List<ResolvedCandidate> findFuzzyFallbackCandidates(
            String entityType,
            String normalizedQuery,
            List<String> queryTokens,
            int limit
    ) {
        if (limit <= 0 || queryTokens.isEmpty()) {
            return List.of();
        }
        var rows = neo4jClient.query(queryFor(entityType, MATCH_TYPE_FUZZY))
                .bind(normalizedQuery).to("normalizedQuery")
                .bind(queryTokens).to("queryTokens")
                .bind(FUZZY_SHORTLIST_LIMIT).to("limit")
                .fetch()
                .all();

        return rows.stream()
                .map(row -> toCandidateSeed(entityType, row))
                .filter(Objects::nonNull)
                .map(seed -> scoreFuzzy(seed, normalizedQuery))
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    var byScore = Double.compare(right.score(), left.score());
                    if (byScore != 0) {
                        return byScore;
                    }
                    return left.name().compareToIgnoreCase(right.name());
                })
                .limit(limit)
                .toList();
    }

    private String queryFor(String entityType, String matchType) {
        return switch (entityType) {
            case ENTITY_TYPE_ALBUM -> albumQuery(matchType);
            case ENTITY_TYPE_TRACK -> trackQuery(matchType);
            case ENTITY_TYPE_ARTIST -> artistQuery(matchType);
            default -> throw new IllegalArgumentException("Unsupported RESOLVE_JAZZLOGS_ENTITY entityType: " + entityType);
        };
    }

    private String albumQuery(String matchType) {
        return switch (matchType) {
            case MATCH_TYPE_EXACT -> """
                    MATCH (album:Album)
                    WHERE album.normalizedName = $normalizedQuery
                    CALL (album) {
                      OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                      WITH DISTINCT artist.name AS artistName
                      WHERE artistName IS NOT NULL
                      ORDER BY artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN album.id AS id,
                           album.name AS name,
                           album.normalizedName AS normalizedName,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_PREFIX -> """
                    MATCH (album:Album)
                    WHERE album.normalizedName STARTS WITH $normalizedQuery
                    CALL (album) {
                      OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                      WITH DISTINCT artist.name AS artistName
                      WHERE artistName IS NOT NULL
                      ORDER BY artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN album.id AS id,
                           album.name AS name,
                           album.normalizedName AS normalizedName,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_CONTAINS -> """
                    MATCH (album:Album)
                    WHERE album.normalizedName CONTAINS $normalizedQuery
                    CALL (album) {
                      OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                      WITH DISTINCT artist.name AS artistName
                      WHERE artistName IS NOT NULL
                      ORDER BY artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN album.id AS id,
                           album.name AS name,
                           album.normalizedName AS normalizedName,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_FUZZY -> """
                    MATCH (album:Album)
                    WHERE any(token IN $queryTokens WHERE album.normalizedName CONTAINS token)
                    CALL (album) {
                      OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                      WITH DISTINCT artist.name AS artistName
                      WHERE artistName IS NOT NULL
                      ORDER BY artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN album.id AS id,
                           album.name AS name,
                           album.normalizedName AS normalizedName,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            default -> throw new IllegalArgumentException("Unsupported match type: " + matchType);
        };
    }

    private String trackQuery(String matchType) {
        return switch (matchType) {
            case MATCH_TYPE_EXACT -> """
                    MATCH (track:Track)
                    WHERE track.normalizedName = $normalizedQuery
                    OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                    CALL (track) {
                      OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                      WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                      WHERE artistName IS NOT NULL
                      ORDER BY position ASC, artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN track.id AS id,
                           track.name AS name,
                           track.normalizedName AS normalizedName,
                           album.name AS album,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_PREFIX -> """
                    MATCH (track:Track)
                    WHERE track.normalizedName STARTS WITH $normalizedQuery
                    OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                    CALL (track) {
                      OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                      WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                      WHERE artistName IS NOT NULL
                      ORDER BY position ASC, artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN track.id AS id,
                           track.name AS name,
                           track.normalizedName AS normalizedName,
                           album.name AS album,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_CONTAINS -> """
                    MATCH (track:Track)
                    WHERE track.normalizedName CONTAINS $normalizedQuery
                    OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                    CALL (track) {
                      OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                      WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                      WHERE artistName IS NOT NULL
                      ORDER BY position ASC, artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN track.id AS id,
                           track.name AS name,
                           track.normalizedName AS normalizedName,
                           album.name AS album,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_FUZZY -> """
                    MATCH (track:Track)
                    WHERE any(token IN $queryTokens WHERE track.normalizedName CONTAINS token)
                    OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
                    CALL (track) {
                      OPTIONAL MATCH (artist:Artist)-[performance:PERFORMED_ON]->(track)
                      WITH artist.name AS artistName, coalesce(performance.position, 999) AS position
                      WHERE artistName IS NOT NULL
                      ORDER BY position ASC, artistName ASC
                      RETURN collect(artistName) AS mainArtists
                    }
                    RETURN track.id AS id,
                           track.name AS name,
                           track.normalizedName AS normalizedName,
                           album.name AS album,
                           mainArtists
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            default -> throw new IllegalArgumentException("Unsupported match type: " + matchType);
        };
    }

    private String artistQuery(String matchType) {
        return switch (matchType) {
            case MATCH_TYPE_EXACT -> """
                    MATCH (artist:Artist)
                    WHERE artist.normalizedName = $normalizedQuery
                    RETURN artist.id AS id,
                           artist.name AS name,
                           artist.normalizedName AS normalizedName
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_PREFIX -> """
                    MATCH (artist:Artist)
                    WHERE artist.normalizedName STARTS WITH $normalizedQuery
                    RETURN artist.id AS id,
                           artist.name AS name,
                           artist.normalizedName AS normalizedName
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_CONTAINS -> """
                    MATCH (artist:Artist)
                    WHERE artist.normalizedName CONTAINS $normalizedQuery
                    RETURN artist.id AS id,
                           artist.name AS name,
                           artist.normalizedName AS normalizedName
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            case MATCH_TYPE_FUZZY -> """
                    MATCH (artist:Artist)
                    WHERE any(token IN $queryTokens WHERE artist.normalizedName CONTAINS token)
                    RETURN artist.id AS id,
                           artist.name AS name,
                           artist.normalizedName AS normalizedName
                    ORDER BY name ASC
                    LIMIT $limit
                    """;
            default -> throw new IllegalArgumentException("Unsupported match type: " + matchType);
        };
    }

    private ResolvedCandidate toResolvedCandidate(String entityType, String matchType, Map<String, Object> row) {
        var seed = toCandidateSeed(entityType, row);
        if (seed == null || seed.id() == null || seed.name() == null) {
            return null;
        }
        return new ResolvedCandidate(
                seed.id(),
                seed.type(),
                seed.name(),
                seed.artistFullName(),
                seed.album(),
                defaultScore(matchType),
                matchType
        );
    }

    private CandidateSeed toCandidateSeed(String entityType, Map<String, Object> row) {
        if (row == null) {
            return null;
        }
        return switch (entityType) {
            case ENTITY_TYPE_ALBUM -> new CandidateSeed(
                    stringValue(row.get("id")),
                    ENTITY_TYPE_ALBUM,
                    stringValue(row.get("name")),
                    String.join(", ", stringList(row.get("mainArtists"))),
                    null,
                    stringValue(row.get("normalizedName"))
            );
            case ENTITY_TYPE_TRACK -> new CandidateSeed(
                    stringValue(row.get("id")),
                    ENTITY_TYPE_TRACK,
                    stringValue(row.get("name")),
                    String.join(", ", stringList(row.get("mainArtists"))),
                    stringValue(row.get("album")),
                    stringValue(row.get("normalizedName"))
            );
            case ENTITY_TYPE_ARTIST -> new CandidateSeed(
                    stringValue(row.get("id")),
                    ENTITY_TYPE_ARTIST,
                    stringValue(row.get("name")),
                    stringValue(row.get("name")),
                    null,
                    stringValue(row.get("normalizedName"))
            );
            default -> null;
        };
    }

    private ResolvedCandidate scoreFuzzy(CandidateSeed seed, String normalizedQuery) {
        if (seed == null || normalizedQuery == null || normalizedQuery.isBlank()) {
            return null;
        }
        var normalizedName = seed.normalizedName() == null ? normalize(seed.name()) : seed.normalizedName();
        if (normalizedName == null || normalizedName.isBlank()) {
            return null;
        }
        var score = levenshteinSimilarity(normalizedQuery, normalizedName);
        if (score < 0.45d) {
            return null;
        }
        return new ResolvedCandidate(
                seed.id(),
                seed.type(),
                seed.name(),
                seed.artistFullName(),
                seed.album(),
                score,
                MATCH_TYPE_FUZZY
        );
    }

    private double defaultScore(String matchType) {
        return switch (matchType) {
            case MATCH_TYPE_EXACT -> 1.0d;
            case MATCH_TYPE_PREFIX -> 0.9d;
            case MATCH_TYPE_CONTAINS -> 0.8d;
            case MATCH_TYPE_FUZZY -> 0.6d;
            default -> 0d;
        };
    }

    private double levenshteinSimilarity(String left, String right) {
        var maxLength = Math.max(left.length(), right.length());
        if (maxLength == 0) {
            return 0d;
        }
        return 1d - ((double) levenshtein(left, right) / (double) maxLength);
    }

    private int levenshtein(String left, String right) {
        var costs = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            costs[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            costs[0] = i;
            int northwest = i - 1;
            for (int j = 1; j <= right.length(); j++) {
                int above = costs[j];
                int value = left.charAt(i - 1) == right.charAt(j - 1)
                        ? northwest
                        : 1 + Math.min(Math.min(costs[j - 1], above), northwest);
                northwest = above;
                costs[j] = value;
            }
        }
        return costs[right.length()];
    }

    private List<String> normalizedTokens(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) {
            return List.of();
        }
        return new ArrayList<>(new LinkedHashSet<>(List.of(normalizedQuery.split("\\s+")))).stream()
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .toList();
    }

    private String renderContent(String entityType, String query, List<ResolvedCandidate> candidates) {
        if (candidates.isEmpty()) {
            return "No JazzLogs entity candidates found for " + entityType + " query \"" + query + "\".";
        }
        var parts = new StringJoiner(" | ");
        parts.add("Resolved " + candidates.size() + " candidate(s) for " + entityType + " query \"" + query + "\".");
        candidates.forEach(candidate -> parts.add(
                "%s %s by %s (id=%s, score=%.2f, match=%s)".formatted(
                        candidate.type(),
                        candidate.name(),
                        candidate.artistFullName() == null || candidate.artistFullName().isBlank()
                                ? "unknown artist"
                                : candidate.artistFullName(),
                        candidate.id(),
                        candidate.score(),
                        candidate.matchType()
                )
        ));
        return parts.toString();
    }

    private String requireEnumLike(Map<String, Object> arguments, String key) {
        var value = requireNonBlank(arguments, key).toUpperCase(Locale.ROOT);
        return switch (value) {
            case ENTITY_TYPE_ALBUM, ENTITY_TYPE_TRACK, ENTITY_TYPE_ARTIST -> value;
            default -> throw new IllegalArgumentException("Unsupported RESOLVE_JAZZLOGS_ENTITY " + key + ": " + value);
        };
    }

    private String requireNonBlank(Map<String, Object> arguments, String key) {
        var value = stringValue(arguments.get(key));
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RESOLVE_JAZZLOGS_ENTITY requires non-blank " + key);
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT);
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    private List<String> stringList(Object value) {
        if (!(value instanceof Iterable<?> iterable)) {
            return List.of();
        }
        var result = new ArrayList<String>();
        for (var item : iterable) {
            var rendered = stringValue(item);
            if (rendered != null) {
                result.add(rendered);
            }
        }
        return List.copyOf(result);
    }

    private record CandidateSeed(
            String id,
            String type,
            String name,
            String artistFullName,
            String album,
            String normalizedName
    ) {
    }

    private record ResolvedCandidate(
            String id,
            String type,
            String name,
            String artistFullName,
            String album,
            double score,
            String matchType
    ) {
        Map<String, Object> toMetadataMap() {
            var map = new LinkedHashMap<String, Object>();
            map.put("id", id);
            map.put("type", type);
            map.put("name", name);
            map.put("artistFullName", artistFullName);
            if (album != null) {
                map.put("album", album);
            }
            map.put("score", score);
            map.put("matchType", matchType);
            return Map.copyOf(map);
        }
    }
}
