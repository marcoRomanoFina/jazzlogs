package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendedItemMetadataResolver {

    private final Neo4jClient neo4jClient;

    public Optional<ResolvedRecommendationMemoryItem> resolve(
            BasicRecommendationTarget recommendationType,
            String winnerNodeId
    ) {
        return resolveAll(recommendationType, List.of(winnerNodeId)).stream()
                .findFirst();
    }

    public List<ResolvedRecommendationMemoryItem> resolveAll(
            BasicRecommendationTarget recommendationType,
            List<String> winnerNodeIds
    ) {
        if (recommendationType == null || winnerNodeIds == null || winnerNodeIds.isEmpty()) {
            return List.of();
        }

        var sanitizedNodeIds = winnerNodeIds.stream()
                .filter(winnerNodeId -> winnerNodeId != null && !winnerNodeId.isBlank())
                .map(String::trim)
                .toList();
        if (sanitizedNodeIds.isEmpty()) {
            return List.of();
        }

        var rows = neo4jClient.query(recommendationType == BasicRecommendationTarget.ALBUM
                        ? albumBatchQuery()
                        : trackBatchQuery())
                .bind(sanitizedNodeIds).to("winnerNodeIds")
                .fetch()
                .all();

        var metadataByNodeId = new LinkedHashMap<String, ArrayDeque<ResolvedRecommendationMemoryItem>>();
        for (var row : rows) {
            var requestedWinnerNodeId = stringValue(row.get("requestedWinnerNodeId"));
            if (requestedWinnerNodeId == null) {
                continue;
            }
            metadataByNodeId.computeIfAbsent(requestedWinnerNodeId, ignored -> new ArrayDeque<>())
                    .add(toMetadata(row));
        }

        var missingWinnerNodeIds = sanitizedNodeIds.stream()
                .filter(winnerNodeId -> {
                    var queue = metadataByNodeId.get(winnerNodeId);
                    return queue == null || queue.isEmpty();
                })
                .toList();
        if (!missingWinnerNodeIds.isEmpty()) {
            throw new RecommendedItemMetadataResolutionException(
                    "Failed to resolve recommended item metadata for winner ids: "
                            + String.join(", ", missingWinnerNodeIds)
            );
        }

        return sanitizedNodeIds.stream()
                .map(metadataByNodeId::get)
                .map(ArrayDeque::removeFirst)
                .toList();
    }

    private String albumBatchQuery() {
        return """
                UNWIND $winnerNodeIds AS requestedWinnerNodeId
                CALL (requestedWinnerNodeId) {
                  MATCH (album:Album)
                  WHERE album.id = requestedWinnerNodeId
                  CALL (album) {
                    OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                    WITH artist.name AS artistName
                    ORDER BY artistName ASC
                    RETURN collect(artistName) AS leaderNames
                  }
                  CALL (album) {
                    OPTIONAL MATCH (album)-[:BELONGS_TO]->(style:Style)
                    WITH style.name AS styleName
                    ORDER BY styleName ASC
                    RETURN collect(styleName) AS styles
                  }
                  CALL (album) {
                    OPTIONAL MATCH (album)-[:EVOKES_MOOD]->(mood:Mood)
                    WITH DISTINCT mood.name AS moodName
                    ORDER BY moodName ASC
                    RETURN collect(moodName) AS moods
                  }
                  CALL (album) {
                    OPTIONAL MATCH (album)-[:CONTAINS]->(track:Track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                    WITH instrument.name AS instrumentName
                    WHERE instrumentName IS NOT NULL
                    ORDER BY instrumentName ASC
                    RETURN head(collect(instrumentName)) AS instrumentFocus
                  }
                  RETURN
                    album.id AS nodeId,
                    album.logNumber AS logNumber,
                    album.name AS album,
                    null AS track,
                    leaderNames,
                    album.spotifyAlbumId AS spotifyAlbumId,
                    null AS spotifyTrackId,
                    album.tier AS tier,
                    album.releaseDate AS releaseDate,
                    head(styles) AS style,
                    album.vocalProfile AS vocalProfile,
                    moods AS moods,
                    album.energy AS energy,
                    album.accessibility AS accessibility,
                    null AS tempoFeel,
                    instrumentFocus AS instrumentFocus,
                    null AS primaryInstrument,
                    null AS importance
                  LIMIT 1
                }
                RETURN requestedWinnerNodeId, nodeId, logNumber, album, track, leaderNames, spotifyAlbumId, spotifyTrackId,
                       tier, releaseDate, style, vocalProfile, moods, energy, accessibility, tempoFeel,
                       instrumentFocus, primaryInstrument, importance
                """;
    }

    private String trackBatchQuery() {
        return """
                UNWIND $winnerNodeIds AS requestedWinnerNodeId
                CALL (requestedWinnerNodeId) {
                  MATCH (track:Track)
                  WHERE track.id = requestedWinnerNodeId
                  OPTIONAL MATCH (album:Album)-[:CONTAINS]->(track)
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
                    ORDER BY styleName ASC
                    RETURN collect(styleName) AS styles
                  }
                  CALL (album) {
                    OPTIONAL MATCH (album)-[:EVOKES_MOOD]->(mood:Mood)
                    WITH DISTINCT mood.name AS moodName
                    ORDER BY moodName ASC
                    RETURN collect(moodName) AS moods
                  }
                  CALL (track) {
                    OPTIONAL MATCH (track)-[:FEATURES_INSTRUMENT]->(instrument:Instrument)
                    WITH DISTINCT instrument.name AS instrumentName
                    ORDER BY instrumentName ASC
                    RETURN collect(instrumentName) AS instruments
                  }
                  RETURN
                    track.id AS nodeId,
                    track.logNumber AS logNumber,
                    album.name AS album,
                    track.name AS track,
                    artistNames,
                    album.spotifyAlbumId AS spotifyAlbumId,
                    track.spotify_track_id AS spotifyTrackId,
                    track.tier AS tier,
                    album.releaseDate AS releaseDate,
                    head(styles) AS style,
                    track.vocalProfile AS vocalProfile,
                    moods AS moods,
                    track.energy AS energy,
                    track.accessibility AS accessibility,
                    track.tempoFeel AS tempoFeel,
                    head(instruments) AS instrumentFocus,
                    null AS primaryInstrument,
                    null AS importance
                  LIMIT 1
                }
                RETURN requestedWinnerNodeId, nodeId, logNumber, album, track, artistNames, spotifyAlbumId, spotifyTrackId,
                       tier, releaseDate, style, vocalProfile, moods, energy, accessibility, tempoFeel,
                       instrumentFocus, primaryInstrument, importance
                """;
    }

    private ResolvedRecommendationMemoryItem toMetadata(Map<String, Object> row) {
        var artists = stringList(row.get("leaderNames"));
        if (artists.isEmpty()) {
            artists = stringList(row.get("artistNames"));
        }

        return new ResolvedRecommendationMemoryItem(
                stringValue(row.get("album")),
                stringValue(row.get("track")),
                artists.isEmpty() ? null : artists.getFirst(),
                releaseYear(row.get("releaseDate")),
                stringValue(row.get("style")),
                stringValue(row.get("vocalProfile")),
                stringList(row.get("moods")),
                stringValue(row.get("energy")),
                stringValue(row.get("accessibility")),
                stringValue(row.get("tempoFeel")),
                stringValue(row.get("instrumentFocus"))
        );
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

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        var rendered = value.toString().trim();
        return rendered.isBlank() ? null : rendered;
    }

    private String releaseYear(Object releaseDate) {
        var value = stringValue(releaseDate);
        if (value == null) {
            return null;
        }
        return value.length() >= 4 ? value.substring(0, 4) : value;
    }
}
