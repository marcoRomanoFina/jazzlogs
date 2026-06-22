package com.marcoromanofinaa.jazzlogs.chat.application;

import com.marcoromanofinaa.jazzlogs.chat.api.dto.RecommendedItemDTO;
import com.marcoromanofinaa.jazzlogs.chat.exchange.ChatExchange;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import com.marcoromanofinaa.jazzlogs.recommendation.orchestration.WinnerReference;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendedItemEnrichmentService {

    private final Neo4jClient neo4jClient;

    public List<RecommendedItemDTO> enrich(BasicRecommendationTarget recommendationType, List<WinnerReference> winners) {
        if (recommendationType == null || winners == null || winners.isEmpty()) {
            return List.of();
        }

        var winnerReferences = requireValidWinnerReferences(recommendationType, winners);
        var dtosByWinnerId = loadDtosByWinnerId(recommendationType, winnerReferences);
        var missingWinnerIds = missingWinnerIds(winnerReferences, dtosByWinnerId);
        if (!missingWinnerIds.isEmpty()) {
            throw new RecommendedItemEnrichmentException(
                    "Failed to enrich recommended items for winner ids: " + String.join(", ", missingWinnerIds)
            );
        }

        return winnerReferences.stream()
                .map(winner -> {
                    var queue = dtosByWinnerId.get(winner.id());
                    return queue.removeFirst();
                })
                .toList();
    }

    public Map<UUID, List<RecommendedItemDTO>> enrichByExchangeId(List<ChatExchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return Map.of();
        }

        var enrichedByExchangeId = new LinkedHashMap<UUID, List<RecommendedItemDTO>>();
        var byType = new LinkedHashMap<BasicRecommendationTarget, List<ChatExchange>>();

        for (var exchange : exchanges) {
            if (exchange == null || exchange.getId() == null) {
                continue;
            }
            if (exchange.getRecommendationType() == null || exchange.getWinners() == null || exchange.getWinners().isEmpty()) {
                enrichedByExchangeId.put(exchange.getId(), List.of());
                continue;
            }
            byType.computeIfAbsent(exchange.getRecommendationType(), ignored -> new java.util.ArrayList<>())
                    .add(exchange);
        }

        for (var entry : byType.entrySet()) {
            var recommendationType = entry.getKey();
            var exchangesForType = entry.getValue();
            var winnerReferences = exchangesForType.stream()
                    .flatMap(exchange -> requireValidWinnerReferences(recommendationType, exchange.getWinners()).stream())
                    .toList();
            var dtosByWinnerId = loadDtosByWinnerId(recommendationType, winnerReferences);
            var missingWinnerIds = missingWinnerIds(winnerReferences, dtosByWinnerId);
            if (!missingWinnerIds.isEmpty()) {
                throw new RecommendedItemEnrichmentException(
                        "Failed to enrich recommended items for winner ids: " + String.join(", ", missingWinnerIds)
                );
            }

            for (var exchange : exchangesForType) {
                var items = requireValidWinnerReferences(recommendationType, exchange.getWinners()).stream()
                        .map(winner -> dtosByWinnerId.get(winner.id()).removeFirst())
                        .toList();
                enrichedByExchangeId.put(exchange.getId(), items);
            }
        }

        return Map.copyOf(enrichedByExchangeId);
    }

    private List<WinnerReference> requireValidWinnerReferences(
            BasicRecommendationTarget recommendationType,
            List<WinnerReference> winners
    ) {
        var winnerReferences = winners.stream()
                .filter(winner -> winner != null)
                .toList();
        if (winnerReferences.size() != winners.size()
                || winnerReferences.stream().anyMatch(winner -> winner.id() == null || winner.id().isBlank())) {
            throw new RecommendedItemEnrichmentException(
                    "Failed to enrich recommended items: one or more winners are missing a valid node id"
            );
        }
        if (winnerReferences.isEmpty()) {
            throw new RecommendedItemEnrichmentException(
                    "Failed to enrich recommended items: no valid winners were provided"
            );
        }
        if (winnerReferences.stream().anyMatch(winner -> winner.type() != recommendationType)) {
            throw new RecommendedItemEnrichmentException(
                    "Failed to enrich recommended items: winner types do not match recommendation type " + recommendationType
            );
        }
        return winnerReferences;
    }

    private LinkedHashMap<String, ArrayDeque<RecommendedItemDTO>> loadDtosByWinnerId(
            BasicRecommendationTarget recommendationType,
            List<WinnerReference> winnerReferences
    ) {
        var rows = neo4jClient.query(recommendationType == BasicRecommendationTarget.ALBUM
                        ? albumQuery()
                        : trackQuery())
                .bind(winnerReferences.stream().map(WinnerReference::id).toList()).to("winnerNodeIds")
                .fetch()
                .all();

        var dtosByWinner = new LinkedHashMap<String, ArrayDeque<RecommendedItemDTO>>();
        for (var row : rows) {
            var requestedWinnerNodeId = stringValue(row.get("requestedWinnerNodeId"));
            if (requestedWinnerNodeId == null) {
                continue;
            }
            dtosByWinner.computeIfAbsent(requestedWinnerNodeId, ignored -> new ArrayDeque<>())
                    .add(toDto(requestedWinnerNodeId, recommendationType, row));
        }
        return dtosByWinner;
    }

    private List<String> missingWinnerIds(
            List<WinnerReference> winnerReferences,
            Map<String, ArrayDeque<RecommendedItemDTO>> dtosByWinner
    ) {
        var missingWinnerIds = winnerReferences.stream()
                .map(WinnerReference::id)
                .filter(winnerId -> {
                    var queue = dtosByWinner.get(winnerId);
                    return queue == null || queue.isEmpty();
                })
                .toList();
        return missingWinnerIds;
    }

    private String albumQuery() {
        return """
                UNWIND $winnerNodeIds AS requestedWinnerNodeId
                CALL (requestedWinnerNodeId) {
                  MATCH (album:Album)
                  WHERE album.id = requestedWinnerNodeId
                  CALL (album) {
                    OPTIONAL MATCH (artist:Artist)-[:LEADER_OF]->(album)
                    WITH artist.name AS artistName
                    WHERE artistName IS NOT NULL
                    ORDER BY artistName ASC
                    RETURN collect(artistName) AS mainArtists
                  }
                  RETURN
                    album.name AS album,
                    null AS track,
                    mainArtists AS mainArtists,
                    album.logNumber AS logNumber,
                    album.tier AS tier,
                    album.releaseDate AS releaseDate,
                    album.spotifyAlbumId AS spotifyAlbumId,
                    null AS spotifyTrackId,
                    album.spotifyUrl AS spotifyUrl,
                    album.imageUrl AS imageUrl,
                    album.instagramPermalink AS instagramPermalink
                  LIMIT 1
                }
                RETURN requestedWinnerNodeId, album, track, mainArtists, logNumber, tier, releaseDate,
                       spotifyAlbumId, spotifyTrackId, spotifyUrl, imageUrl, instagramPermalink
                """;
    }

    private String trackQuery() {
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
                    RETURN collect(artistName) AS mainArtists
                  }
                  RETURN
                    album.name AS album,
                    track.name AS track,
                    mainArtists AS mainArtists,
                    track.logNumber AS logNumber,
                    track.tier AS tier,
                    album.releaseDate AS releaseDate,
                    album.spotifyAlbumId AS spotifyAlbumId,
                    track.spotify_track_id AS spotifyTrackId,
                    track.spotifyUrl AS spotifyUrl,
                    album.imageUrl AS imageUrl,
                    album.instagramPermalink AS instagramPermalink
                  LIMIT 1
                }
                RETURN requestedWinnerNodeId, album, track, mainArtists, logNumber, tier, releaseDate,
                       spotifyAlbumId, spotifyTrackId, spotifyUrl, imageUrl, instagramPermalink
                """;
    }

    private RecommendedItemDTO toDto(
            String winnerNodeId,
            BasicRecommendationTarget recommendationType,
            Map<String, Object> row
    ) {
        var mainArtists = stringList(row.get("mainArtists"));
        return new RecommendedItemDTO(
                winnerNodeId,
                recommendationType == BasicRecommendationTarget.ALBUM
                        ? BasicRecommendationTarget.ALBUM
                        : BasicRecommendationTarget.TRACKS,
                stringValue(row.get("album")),
                stringValue(row.get("track")),
                mainArtists,
                integerValue(row.get("logNumber")),
                stringValue(row.get("tier")),
                releaseYear(row.get("releaseDate")),
                stringValue(row.get("spotifyAlbumId")),
                stringValue(row.get("spotifyTrackId")),
                stringValue(row.get("spotifyUrl")),
                stringValue(row.get("imageUrl")),
                stringValue(row.get("instagramPermalink"))
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

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String releaseYear(Object releaseDate) {
        var value = stringValue(releaseDate);
        if (value == null) {
            return null;
        }
        return value.length() >= 4 ? value.substring(0, 4) : value;
    }
}
