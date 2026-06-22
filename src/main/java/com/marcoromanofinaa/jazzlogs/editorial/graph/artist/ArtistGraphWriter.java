package com.marcoromanofinaa.jazzlogs.editorial.graph.artist;

import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.ArtistEditorialData;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.ArtistNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ArtistGraphWriter {

    private final Neo4jClient neo4jClient;

    @Transactional
    public ArtistNode upsertFromSpotify(String spotifyArtistId, String name, String spotifyUrl) {
        var row = neo4jClient.query("""
                MERGE (artist:Artist {spotifyArtistId: $spotifyArtistId})
                ON CREATE SET artist.id = randomUUID()
                SET artist.name = $name,
                    artist.normalizedName = $normalizedName,
                    artist.spotifyUrl = $spotifyUrl
                RETURN artist.id AS id,
                       artist.spotifyArtistId AS spotifyArtistId,
                       artist.name AS name,
                       artist.spotifyUrl AS spotifyUrl
                """)
                .bind(spotifyArtistId).to("spotifyArtistId")
                .bind(name).to("name")
                .bind(normalizeName(name)).to("normalizedName")
                .bind(spotifyUrl).to("spotifyUrl")
                .fetch()
                .one()
                .orElseThrow();

        return ArtistNode.builder()
                .id((String) row.get("id"))
                .spotifyArtistId((String) row.get("spotifyArtistId"))
                .name((String) row.get("name"))
                .spotifyUrl((String) row.get("spotifyUrl"))
                .build();
    }

    @Transactional
    public ArtistNode upsertEditorial(ArtistEditorialData artistData) {
        var artistId = findExistingArtistId(artistData.spotifyArtistId(), artistData.artistName())
                .orElseGet(this::newArtistId);
        var savedArtist = mergeEditorialArtistNode(artistId, artistData);
        replacePrimaryInstrument(savedArtist.getId(), artistData.primaryInstrument());
        replaceNamedRelationships(savedArtist.getId(), "HAS_STYLE", "Style", artistData.mainStyles());
        replaceNamedRelationships(savedArtist.getId(), "PERFECT_FOR", "Context", artistData.bestListeningMoments());
        replaceRelatedArtists(savedArtist.getId(), artistData.relatedArtists());
        assignEntryPoints(savedArtist, artistData.entryPointLogNumbers());
        return savedArtist;
    }

    private Optional<String> findExistingArtistId(String spotifyArtistId, String artistName) {
        if ((spotifyArtistId == null || spotifyArtistId.isBlank())
                && (artistName == null || artistName.isBlank())) {
            return Optional.empty();
        }
        return neo4jClient.query("""
                MATCH (artist:Artist)
                WHERE ($spotifyArtistId IS NOT NULL AND artist.spotifyArtistId = $spotifyArtistId)
                   OR ($artistName IS NOT NULL AND toLower(artist.name) = toLower($artistName))
                RETURN artist.id AS id
                LIMIT 1
                """)
                .bind(blankToNull(spotifyArtistId)).to("spotifyArtistId")
                .bind(blankToNull(artistName)).to("artistName")
                .fetch()
                .one()
                .map(row -> (String) row.get("id"));
    }

    private ArtistNode mergeEditorialArtistNode(String artistId, ArtistEditorialData artistData) {
        var row = neo4jClient.query("""
                MERGE (artist:Artist {id: $artistId})
                SET artist.spotifyArtistId = $spotifyArtistId,
                    artist.name = $artistName,
                    artist.normalizedName = $normalizedName,
                    artist.signatureSound = $signatureSound,
                    artist.artistContext = $artistContext,
                    artist.jazzlogsTake = $jazzlogsTake,
                    artist.avoidIf = $avoidIf,
                    artist.editorialImportance = $editorialImportance,
                    artist.logAppearances = $logAppearances
                RETURN artist.id AS id,
                       artist.spotifyArtistId AS spotifyArtistId,
                       artist.name AS name
                """)
                .bind(artistId).to("artistId")
                .bind(artistData.spotifyArtistId().trim()).to("spotifyArtistId")
                .bind(artistData.artistName().trim()).to("artistName")
                .bind(normalizeName(artistData.artistName())).to("normalizedName")
                .bind(artistData.soundProfile()).to("signatureSound")
                .bind(artistData.artistContext()).to("artistContext")
                .bind(artistData.editorialNote()).to("jazzlogsTake")
                .bind(artistData.avoidIf()).to("avoidIf")
                .bind(artistData.whyItMatters()).to("editorialImportance")
                .bind(defaultIntegerList(artistData.appearsInLogs())).to("logAppearances")
                .fetch()
                .one()
                .orElseThrow();

        return ArtistNode.builder()
                .id((String) row.get("id"))
                .spotifyArtistId((String) row.get("spotifyArtistId"))
                .name((String) row.get("name"))
                .build();
    }

    private void replacePrimaryInstrument(String artistId, String instrumentName) {
        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                OPTIONAL MATCH (artist)-[relationship:PLAYS_INSTRUMENT]->(:Instrument)
                DELETE relationship
                """)
                .bind(artistId).to("artistId")
                .run();

        if (instrumentName == null || instrumentName.isBlank()) {
            return;
        }

        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                MERGE (instrument:Instrument {name: $instrumentName})
                MERGE (artist)-[:PLAYS_INSTRUMENT]->(instrument)
                """)
                .bind(artistId).to("artistId")
                .bind(instrumentName.trim()).to("instrumentName")
                .run();
    }

    private void replaceNamedRelationships(String artistId, String relationshipType, String label, List<String> values) {
        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                OPTIONAL MATCH (artist)-[relationship:%s]->(:%s)
                DELETE relationship
                """.formatted(relationshipType, label))
                .bind(artistId).to("artistId")
                .run();

        var validValues = distinctNonBlank(values);
        if (validValues.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                UNWIND $values AS value
                MERGE (target:%s {name: value})
                MERGE (artist)-[:%s]->(target)
                """.formatted(label, relationshipType))
                .bind(artistId).to("artistId")
                .bind(validValues).to("values")
                .run();
    }

    private void replaceRelatedArtists(String artistId, List<String> relatedArtists) {
        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                OPTIONAL MATCH (artist)-[relationship:SIMILAR_TO]->(:Artist)
                DELETE relationship
                """)
                .bind(artistId).to("artistId")
                .run();

        var validNames = distinctNonBlank(relatedArtists);
        if (validNames.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (artist:Artist {id: $artistId})
                UNWIND $relatedArtistNames AS relatedArtistName
                MERGE (relatedArtist:Artist {name: relatedArtistName})
                ON CREATE SET relatedArtist.id = randomUUID()
                MERGE (artist)-[:SIMILAR_TO]->(relatedArtist)
                """)
                .bind(artistId).to("artistId")
                .bind(validNames).to("relatedArtistNames")
                .run();
    }

    private String newArtistId() {
        return java.util.UUID.randomUUID().toString();
    }

    private String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        var trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }

    private List<Integer> defaultIntegerList(List<Integer> values) {
        return values == null ? List.of() : values;
    }

    private void assignEntryPoints(ArtistNode artist, List<Integer> entryPointLogNumbers) {
        if (entryPointLogNumbers == null || entryPointLogNumbers.isEmpty()) {
            return;
        }

        for (Integer logNumber : entryPointLogNumbers.stream().filter(java.util.Objects::nonNull).distinct().toList()) {
            assignEntryPointAlbum(artist, logNumber);
            assignEntryPointTrack(artist, logNumber);
        }
    }

    private void assignEntryPointAlbum(ArtistNode artist, Integer logNumber) {
        neo4jClient.query("""
                MATCH (album:Album {logNumber: $logNumber})
                MATCH (artist:Artist {id: $artistId})
                MERGE (album)-[:ENTRY_POINT_TO]->(artist)
                """)
                .bind(logNumber).to("logNumber")
                .bind(artist.getId()).to("artistId")
                .run();
    }

    private void assignEntryPointTrack(ArtistNode artist, Integer logNumber) {
        neo4jClient.query("""
                MATCH (track:Track {logNumber: $logNumber})
                MATCH (artist:Artist {id: $artistId})
                MERGE (track)-[:ENTRY_POINT_TO]->(artist)
                """)
                .bind(logNumber).to("logNumber")
                .bind(artist.getId()).to("artistId")
                .run();
    }

    private List<String> distinctNonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        var ordered = new LinkedHashMap<String, String>();
        for (var value : values) {
            if (value == null) {
                continue;
            }
            var trimmed = value.trim();
            if (!trimmed.isBlank()) {
                ordered.putIfAbsent(trimmed, trimmed);
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
