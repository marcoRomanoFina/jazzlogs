package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.InstrumentNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.MoodNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.StyleNode;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.UserNode;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesOptionsDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesRequestDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.DiscoveryMode;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.JazzExperienceLevel;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.TempoFeel;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserGraphPreferencesService {

    private final Neo4jClient neo4jClient;

    public UserNode createUserNode(User user) {
        return mergeUserNode(user);
    }

    public UserNode ensureUserNode(User user) {
        return mergeUserNode(user);
    }

    public UserNode upsertPreferences(User user, UserJazzPreferencesRequestDto preferences) {
        var ensuredUser = ensureUserNode(user);
        var userId = ensuredUser.getUserId();

        var jazzExperienceLevel = parseRequiredEnum(preferences.jazzExperienceLevel(), JazzExperienceLevel.class);
        var discoveryMode = parseRequiredEnum(preferences.discoveryMode(), DiscoveryMode.class);
        var tempoFeel = parseRequiredEnum(preferences.tempoFeel(), TempoFeel.class);

        neo4jClient.query("""
                MERGE (user:User {userId: $userId})
                SET user.jazzExperienceLevel = $jazzExperienceLevel,
                    user.discoveryMode = $discoveryMode,
                    user.likesVocals = $likesVocals,
                    user.preferredTempoFeel = $preferredTempoFeel
                """)
                .bind(userId).to("userId")
                .bind(jazzExperienceLevel.name()).to("jazzExperienceLevel")
                .bind(discoveryMode.name()).to("discoveryMode")
                .bind(preferences.likesVocals()).to("likesVocals")
                .bind(tempoFeel.name()).to("preferredTempoFeel")
                .run();

        replaceRelationshipsById(userId, "LIKES_ARTIST", "Artist", "name", sanitizeIds(preferences.favoriteArtists()));
        replaceRelationshipsById(userId, "LIKES_STYLE", "Style", "name", sanitizeIds(preferences.preferredSubgenres()));
        replaceRelationshipsById(userId, "PREFERS_MOOD", "Mood", "name", sanitizeIds(preferences.preferredMoods()));
        replaceRelationshipsById(userId, "LIKES_INSTRUMENT", "Instrument", "name", sanitizeIds(preferences.favoriteInstruments()));

        return findByUserId(user.getId()).orElseThrow();
    }

    public Optional<UserNode> findByUserId(UUID userId) {
        return neo4jClient.query("""
                MATCH (user:User {userId: $userId})
                OPTIONAL MATCH (user)-[:LIKES_ARTIST]->(artist:Artist)
                WITH user, collect(DISTINCT {id: artist.id, name: artist.name}) AS favoriteArtists
                OPTIONAL MATCH (user)-[:LIKES_INSTRUMENT]->(instrument:Instrument)
                WITH user, favoriteArtists, collect(DISTINCT {id: instrument.name, name: instrument.name}) AS favoriteInstruments
                OPTIONAL MATCH (user)-[:LIKES_STYLE]->(style:Style)
                WITH user, favoriteArtists, favoriteInstruments, collect(DISTINCT {id: style.name, name: style.name}) AS preferredStyles
                OPTIONAL MATCH (user)-[:PREFERS_MOOD]->(mood:Mood)
                WITH user, favoriteArtists, favoriteInstruments, preferredStyles, collect(DISTINCT {id: mood.name, name: mood.name}) AS preferredMoods
                RETURN user.userId AS userId,
                       user.displayName AS displayName,
                       user.jazzExperienceLevel AS jazzExperienceLevel,
                       user.discoveryMode AS discoveryMode,
                       user.likesVocals AS likesVocals,
                       user['preferredTempoFeel'] AS preferredTempoFeel,
                       favoriteArtists,
                       favoriteInstruments,
                       preferredStyles,
                       preferredMoods
                LIMIT 1
                """)
                .bind(userId.toString()).to("userId")
                .fetch()
                .one()
                .map(this::toUserNode);
    }

    public UserJazzPreferencesOptionsDto getPreferencesOptions() {
        return new UserJazzPreferencesOptionsDto(
                fetchArtistOptions(),
                fetchNameOptions("MATCH (style:Style)", "style.name"),
                fetchNameOptions("MATCH (mood:Mood)", "mood.name"),
                fetchNameOptions("MATCH (instrument:Instrument)", "instrument.name"),
                enumOptions(TempoFeel.values()),
                enumOptions(DiscoveryMode.values()),
                enumOptions(JazzExperienceLevel.values())
        );
    }

    public boolean hasPreferences(UUID userId) {
        return neo4jClient.query("""
                MATCH (user:User {userId: $userId})
                RETURN user.jazzExperienceLevel IS NOT NULL
                    OR user.discoveryMode IS NOT NULL
                    OR user.likesVocals IS NOT NULL
                    OR user['preferredTempoFeel'] IS NOT NULL
                    OR EXISTS { MATCH (user)-[:LIKES_ARTIST]->(:Artist) }
                    OR EXISTS { MATCH (user)-[:LIKES_INSTRUMENT]->(:Instrument) }
                    OR EXISTS { MATCH (user)-[:LIKES_STYLE]->(:Style) }
                    OR EXISTS { MATCH (user)-[:PREFERS_MOOD]->(:Mood) }
                    AS hasPreferences
                LIMIT 1
                """)
                .bind(userId.toString()).to("userId")
                .fetchAs(Boolean.class)
                .mappedBy((typeSystem, record) -> record.get("hasPreferences").asBoolean())
                .one()
                .orElse(false);
    }

    public boolean hasPreferences(UserNode userNode) {
        return userNode.getJazzExperienceLevel() != null
                || userNode.getDiscoveryMode() != null
                || userNode.getLikesVocals() != null
                || !userNode.getFavoriteArtists().isEmpty()
                || !userNode.getFavoriteInstruments().isEmpty()
                || !userNode.getPreferredStyles().isEmpty()
                || !userNode.getPreferredMoods().isEmpty()
                || userNode.getPreferredTempoFeel() != null;
    }

    private UserNode mergeUserNode(User user) {
        var row = neo4jClient.query("""
                MERGE (user:User {userId: $userId})
                SET user.displayName = $displayName
                RETURN user.userId AS userId, user.displayName AS displayName
                """)
                .bind(user.getId().toString()).to("userId")
                .bind(user.getDisplayName()).to("displayName")
                .fetch()
                .one()
                .orElseThrow();

        return UserNode.builder()
                .userId((String) row.get("userId"))
                .displayName((String) row.get("displayName"))
                .build();
    }

    private void replaceRelationshipsById(
            String userId,
            String relationshipType,
            String label,
            String idProperty,
            List<String> ids
    ) {
        validateTargetIdsExist(label, idProperty, ids);

        neo4jClient.query("""
                MATCH (user:User {userId: $userId})
                OPTIONAL MATCH (user)-[relationship:%s]->(:%s)
                DELETE relationship
                """.formatted(relationshipType, label))
                .bind(userId).to("userId")
                .run();

        if (ids.isEmpty()) {
            return;
        }

        var row = neo4jClient.query("""
                MATCH (user:User {userId: $userId})
                UNWIND $ids AS targetId
                MATCH (target:%s)
                WHERE toLower(trim(target.%s)) = toLower(trim(targetId))
                WITH user, targetId, target
                MERGE (user)-[:%s]->(target)
                RETURN count(DISTINCT toLower(trim(targetId))) AS matchedCount
                """.formatted(label, idProperty, relationshipType))
                .bind(userId).to("userId")
                .bind(ids).to("ids")
                .fetch()
                .one()
                .orElseThrow();

        var matchedCount = ((Number) row.get("matchedCount")).intValue();
        if (matchedCount != ids.size()) {
            throw new IllegalArgumentException(
                    "Some selected preference ids do not exist for %s. expected=%d matched=%d"
                            .formatted(label, ids.size(), matchedCount)
            );
        }
    }

    private void validateTargetIdsExist(String label, String idProperty, List<String> ids) {
        if (ids.isEmpty()) {
            return;
        }

        var row = neo4jClient.query("""
                UNWIND $ids AS targetId
                MATCH (target:%s)
                WHERE toLower(trim(target.%s)) = toLower(trim(targetId))
                RETURN count(DISTINCT toLower(trim(targetId))) AS matchedCount
                """.formatted(label, idProperty))
                .bind(ids).to("ids")
                .fetch()
                .one()
                .orElseThrow();

        var matchedCount = ((Number) row.get("matchedCount")).intValue();
        if (matchedCount != ids.size()) {
            throw new IllegalArgumentException(
                    "Some selected preference ids do not exist for %s. expected=%d matched=%d"
                            .formatted(label, ids.size(), matchedCount)
            );
        }
    }

    private UserNode toUserNode(Map<String, Object> row) {
        return UserNode.builder()
                .userId((String) row.get("userId"))
                .displayName((String) row.get("displayName"))
                .jazzExperienceLevel((String) row.get("jazzExperienceLevel"))
                .discoveryMode((String) row.get("discoveryMode"))
                .likesVocals((Boolean) row.get("likesVocals"))
                .favoriteArtists(toArtistNodes((List<?>) row.get("favoriteArtists")))
                .favoriteInstruments(toInstrumentNodes((List<?>) row.get("favoriteInstruments")))
                .preferredStyles(toStyleNodes((List<?>) row.get("preferredStyles")))
                .preferredMoods(toMoodNodes((List<?>) row.get("preferredMoods")))
                .preferredTempoFeel((String) row.get("preferredTempoFeel"))
                .build();
    }

    private List<String> fetchArtistOptions() {
        return fetchNameOptions("MATCH (artist:Artist)-[:LEADER_OF]->(:Album)", "artist.name");
    }

    private List<String> fetchNameOptions(String matchClause, String nameExpression) {
        return List.copyOf(neo4jClient.query("""
                %s
                WHERE %s IS NOT NULL AND trim(%s) <> ''
                RETURN DISTINCT %s AS name
                ORDER BY name ASC
                """.formatted(matchClause, nameExpression, nameExpression, nameExpression))
                .fetchAs(String.class)
                .mappedBy((typeSystem, record) -> record.get("name").asString())
                .all());
    }

    private <E extends Enum<E>> List<String> enumOptions(E[] values) {
        return java.util.Arrays.stream(values)
                .map(this::enumLabel)
                .toList();
    }

    private <E extends Enum<E>> E parseRequiredEnum(String rawValue, Class<E> enumType) {
        var normalizedValue = rawValue == null ? "" : rawValue.trim();
        try {
            return Enum.valueOf(enumType, normalizedValue.toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            return java.util.Arrays.stream(enumType.getEnumConstants())
                    .filter(value -> enumLabel(value).equalsIgnoreCase(normalizedValue))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Invalid %s id: %s".formatted(enumType.getSimpleName(), rawValue),
                            exception
                    ));
        }
    }

    private List<String> sanitizeIds(List<String> rawIds) {
        if (rawIds == null || rawIds.isEmpty()) {
            return List.of();
        }
        var ids = new LinkedHashMap<String, String>();
        for (var rawId : rawIds) {
            if (rawId == null) {
                continue;
            }
            var rendered = rawId.trim();
            if (!rendered.isBlank()) {
                ids.putIfAbsent(rendered, rendered);
            }
        }
        return List.copyOf(ids.values());
    }

    private java.util.Set<ArtistNode> toArtistNodes(List<?> rawValues) {
        var nodes = new LinkedHashSet<ArtistNode>();
        for (var value : sanitizeOptionValues(rawValues)) {
            nodes.add(ArtistNode.builder()
                    .id(value.id())
                    .name(value.name())
                    .normalizedName(value.name().toLowerCase(Locale.ROOT))
                    .build());
        }
        return nodes;
    }

    private java.util.Set<InstrumentNode> toInstrumentNodes(List<?> rawValues) {
        var nodes = new LinkedHashSet<InstrumentNode>();
        for (var value : sanitizeOptionValues(rawValues)) {
            nodes.add(InstrumentNode.builder().name(value.name()).build());
        }
        return nodes;
    }

    private java.util.Set<StyleNode> toStyleNodes(List<?> rawValues) {
        var nodes = new LinkedHashSet<StyleNode>();
        for (var value : sanitizeOptionValues(rawValues)) {
            nodes.add(StyleNode.builder().id(value.id()).name(value.name()).build());
        }
        return nodes;
    }

    private java.util.Set<MoodNode> toMoodNodes(List<?> rawValues) {
        var nodes = new LinkedHashSet<MoodNode>();
        for (var value : sanitizeOptionValues(rawValues)) {
            nodes.add(MoodNode.builder().id(value.id()).name(value.name()).build());
        }
        return nodes;
    }

    private List<OptionValue> sanitizeOptionValues(List<?> rawValues) {
        if (rawValues == null || rawValues.isEmpty()) {
            return List.of();
        }
        var values = new LinkedHashMap<String, OptionValue>();
        for (var rawValue : rawValues) {
            if (!(rawValue instanceof Map<?, ?> map)) {
                continue;
            }
            var id = map.get("id");
            var name = map.get("name");
            if (id == null || name == null) {
                continue;
            }
            var renderedId = id.toString().trim();
            var renderedName = name.toString().trim();
            if (!renderedId.isBlank() && !renderedName.isBlank()) {
                values.putIfAbsent(renderedId, new OptionValue(renderedId, renderedName));
            }
        }
        return List.copyOf(values.values());
    }

    private String enumLabel(Enum<?> value) {
        if (value instanceof TempoFeel tempoFeel) {
            return tempoFeel.label();
        }
        if (value instanceof DiscoveryMode discoveryMode) {
            return discoveryMode.label();
        }
        if (value instanceof JazzExperienceLevel jazzExperienceLevel) {
            return jazzExperienceLevel.label();
        }
        return value.name();
    }

    private record OptionValue(String id, String name) {
    }
}
