package com.marcoromanofinaa.jazzlogs.editorial.graph.track;

import com.marcoromanofinaa.jazzlogs.admin.editorial.track.TrackEditorialData;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TrackGraphWriter {

    private final Neo4jClient neo4jClient;

    @Transactional
    public TrackNode upsertFromSpotify(
            String spotifyTrackId,
            String name,
            Integer durationMs,
            String spotifyUrl
    ) {
        var row = neo4jClient.query("""
                MERGE (track:Track {spotify_track_id: $spotifyTrackId})
                ON CREATE SET track.id = randomUUID()
                SET track.name = $name,
                    track.normalizedName = $normalizedName,
                    track.durationMs = $durationMs,
                    track.spotifyUrl = $spotifyUrl
                RETURN track.id AS id,
                       track.spotify_track_id AS spotifyTrackId,
                       track.name AS name,
                       track.durationMs AS durationMs,
                       track.spotifyUrl AS spotifyUrl
                """)
                .bind(spotifyTrackId).to("spotifyTrackId")
                .bind(name).to("name")
                .bind(normalizeName(name)).to("normalizedName")
                .bind(durationMs).to("durationMs")
                .bind(spotifyUrl).to("spotifyUrl")
                .fetch()
                .one()
                .orElseThrow();

        return TrackNode.builder()
                .id((String) row.get("id"))
                .spotifyTrackId((String) row.get("spotifyTrackId"))
                .name((String) row.get("name"))
                .durationMs(toInteger(row.get("durationMs")))
                .spotifyUrl((String) row.get("spotifyUrl"))
                .build();
    }

    @Transactional
    public TrackNode upsertEditorial(TrackEditorialData trackData) {
        var row = neo4jClient.query("""
                MERGE (track:Track {spotify_track_id: $spotifyTrackId})
                ON CREATE SET track.id = randomUUID()
                SET track.logNumber = $logNumber,
                    track.name = $trackName,
                    track.normalizedName = $normalizedName,
                    track.isStandout = $standout,
                    track.tier = $tier,
                    track.vocalProfile = $vocalProfile,
                    track.energy = $energy,
                    track.moodIntensity = $moodIntensity,
                    track.accessibility = $accessibility,
                    track.tempoFeel = $tempoFeel,
                    track.compositionType = $compositionType,
                    track.bestMoment = $bestMoment,
                    track.whyItHits = $whyItHits,
                    track.editorialNote = $editorialNote,
                    track.recommendedIf = $recommendedIf,
                    track.avoidIf = $avoidIf,
                    track.vocalStyle = $vocalStyle
                RETURN track.id AS id,
                       track.spotify_track_id AS spotifyTrackId,
                       track.logNumber AS logNumber,
                       track.name AS name
                """)
                .bind(trackData.spotifyTrackId()).to("spotifyTrackId")
                .bind(trackData.logNumber()).to("logNumber")
                .bind(trackData.trackName()).to("trackName")
                .bind(normalizeName(trackData.trackName())).to("normalizedName")
                .bind(trackData.standout()).to("standout")
                .bind(trackData.tier()).to("tier")
                .bind(trackData.vocalProfile()).to("vocalProfile")
                .bind(trackData.energy()).to("energy")
                .bind(trackData.moodIntensity()).to("moodIntensity")
                .bind(trackData.accessibility()).to("accessibility")
                .bind(trackData.tempoFeel()).to("tempoFeel")
                .bind(trackData.compositionType()).to("compositionType")
                .bind(trackData.bestMoment()).to("bestMoment")
                .bind(trackData.whyItHits()).to("whyItHits")
                .bind(trackData.editorialNote()).to("editorialNote")
                .bind(trackData.recommendedIf()).to("recommendedIf")
                .bind(trackData.avoidIf()).to("avoidIf")
                .bind(trackData.vocalStyle()).to("vocalStyle")
                .fetch()
                .one()
                .orElseThrow();

        replaceNamedRelationships(trackData.spotifyTrackId(), "EVOKES_MOOD", "Mood", trackData.moods());
        replaceNamedRelationships(trackData.spotifyTrackId(), "PERFECT_FOR", "Context", trackData.listeningContext());
        replaceNamedRelationships(trackData.spotifyTrackId(), "FEATURES_INSTRUMENT", "Instrument", trackData.instruments());
        replaceSingleNamedRelationship(trackData.spotifyTrackId(), "HAS_RHYTHM", "Rhythm", trackData.rhythmFeel());
        updateAlbumPlacementRole(trackData.spotifyTrackId(), trackData.spotifyAlbumId(), trackData.albumName(), trackData.albumRole());

        return TrackNode.builder()
                .id((String) row.get("id"))
                .spotifyTrackId((String) row.get("spotifyTrackId"))
                .logNumber(toInteger(row.get("logNumber")))
                .name((String) row.get("name"))
                .build();
    }

    private void updateAlbumPlacementRole(
            String spotifyTrackId,
            String spotifyAlbumId,
            String albumName,
            String albumRole
    ) {
        if (spotifyTrackId == null || spotifyTrackId.isBlank()) {
            return;
        }

        if (spotifyAlbumId != null && !spotifyAlbumId.isBlank()) {
            neo4jClient.query("""
                    MATCH (album:Album {spotifyAlbumId: $spotifyAlbumId})-[relationship:CONTAINS]->(track:Track {spotify_track_id: $spotifyTrackId})
                    SET relationship.trackRole = $albumRole
                    """)
                    .bind(spotifyAlbumId).to("spotifyAlbumId")
                    .bind(spotifyTrackId).to("spotifyTrackId")
                    .bind(albumRole).to("albumRole")
                    .run();
            return;
        }

        if (albumName != null && !albumName.isBlank()) {
            neo4jClient.query("""
                    MATCH (album:Album)
                    WHERE toLower(album.name) = toLower($albumName)
                    MATCH (album)-[relationship:CONTAINS]->(track:Track {spotify_track_id: $spotifyTrackId})
                    SET relationship.trackRole = $albumRole
                    """)
                    .bind(albumName).to("albumName")
                    .bind(spotifyTrackId).to("spotifyTrackId")
                    .bind(albumRole).to("albumRole")
                    .run();
        }
    }

    private void replaceSingleNamedRelationship(String spotifyTrackId, String relationshipType, String label, String value) {
        neo4jClient.query("""
                MATCH (track:Track {spotify_track_id: $spotifyTrackId})
                OPTIONAL MATCH (track)-[relationship:%s]->(:%s)
                DELETE relationship
                """.formatted(relationshipType, label))
                .bind(spotifyTrackId).to("spotifyTrackId")
                .run();

        if (value == null || value.isBlank()) {
            return;
        }

        neo4jClient.query("""
                MATCH (track:Track {spotify_track_id: $spotifyTrackId})
                UNWIND $values AS value
                MERGE (target:%s {name: value})
                MERGE (track)-[:%s]->(target)
                """.formatted(label, relationshipType))
                .bind(spotifyTrackId).to("spotifyTrackId")
                .bind(List.of(value.trim())).to("values")
                .run();
    }

    private void replaceNamedRelationships(String spotifyTrackId, String relationshipType, String label, List<String> values) {
        neo4jClient.query("""
                MATCH (track:Track {spotify_track_id: $spotifyTrackId})
                OPTIONAL MATCH (track)-[relationship:%s]->(:%s)
                DELETE relationship
                """.formatted(relationshipType, label))
                .bind(spotifyTrackId).to("spotifyTrackId")
                .run();

        var validValues = distinctNonBlank(values);
        if (validValues.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (track:Track {spotify_track_id: $spotifyTrackId})
                UNWIND $values AS value
                MERGE (target:%s {name: value})
                MERGE (track)-[:%s]->(target)
                """.formatted(label, relationshipType))
                .bind(spotifyTrackId).to("spotifyTrackId")
                .bind(validValues).to("values")
                .run();
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return Math.toIntExact(longValue);
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Unsupported numeric value type: " + value.getClass().getName());
    }

    private List<String> distinctNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        var ordered = new LinkedHashMap<String, String>();
        for (var value : values) {
            if (value != null) {
                var trimmed = value.trim();
                if (!trimmed.isBlank()) {
                    ordered.putIfAbsent(trimmed, trimmed);
                }
            }
        }
        return List.copyOf(ordered.values());
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
