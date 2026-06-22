package com.marcoromanofinaa.jazzlogs.editorial.graph.album;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumEditorialData;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumEditorialMainArtist;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumEditorialPersonnel;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AlbumGraphWriter {

    private final Neo4jClient neo4jClient;

    @Transactional
    public AlbumNode upsertFromSpotify(
            String spotifyAlbumId,
            String name,
            String releaseDate,
            Integer totalTracks,
            String imageUrl,
            String spotifyUrl
    ) {
        var row = neo4jClient.query("""
                MERGE (album:Album {spotifyAlbumId: $spotifyAlbumId})
                ON CREATE SET album.id = randomUUID()
                SET album.name = $name,
                    album.normalizedName = $normalizedName,
                    album.releaseDate = $releaseDate,
                    album.totalTracks = $totalTracks,
                    album.imageUrl = $imageUrl,
                    album.spotifyUrl = $spotifyUrl
                RETURN album.id AS id,
                       album.spotifyAlbumId AS spotifyAlbumId,
                       album.name AS name,
                       album.releaseDate AS releaseDate,
                       album.totalTracks AS totalTracks,
                       album.imageUrl AS imageUrl,
                       album.spotifyUrl AS spotifyUrl
                """)
                .bind(spotifyAlbumId).to("spotifyAlbumId")
                .bind(name).to("name")
                .bind(normalizeName(name)).to("normalizedName")
                .bind(releaseDate).to("releaseDate")
                .bind(totalTracks).to("totalTracks")
                .bind(imageUrl).to("imageUrl")
                .bind(spotifyUrl).to("spotifyUrl")
                .fetch()
                .one()
                .orElseThrow();

        return AlbumNode.builder()
                .id((String) row.get("id"))
                .spotifyAlbumId((String) row.get("spotifyAlbumId"))
                .name((String) row.get("name"))
                .releaseDate((String) row.get("releaseDate"))
                .totalTracks(toInteger(row.get("totalTracks")))
                .imageUrl((String) row.get("imageUrl"))
                .spotifyUrl((String) row.get("spotifyUrl"))
                .build();
    }

    @Transactional
    public AlbumNode upsertEditorial(AlbumEditorialData albumData) {
        var savedAlbum = mergeEditorialAlbumNode(albumData);
        replaceLeaderArtists(savedAlbum.getId(), albumData.mainArtists());
        replaceNamedRelationships(savedAlbum.getId(), "BELONGS_TO", "Style", albumData.styles());
        replaceNamedRelationships(savedAlbum.getId(), "EVOKES_MOOD", "Mood", albumData.moods());
        replaceNamedRelationships(savedAlbum.getId(), "PERFECT_FOR", "Context", albumData.listeningContext());
        replaceSidemen(savedAlbum.getId(), albumData);
        return savedAlbum;
    }

    private AlbumNode mergeEditorialAlbumNode(AlbumEditorialData albumData) {
        var row = neo4jClient.query("""
                MERGE (album:Album {spotifyAlbumId: $spotifyAlbumId})
                ON CREATE SET album.id = randomUUID()
                SET album.logNumber = $logNumber,
                    album.name = $albumName,
                    album.normalizedName = $normalizedName,
                    album.postedAt = $postedAt,
                    album.instagramPermalink = $instagramPermalink,
                    album.vocalProfile = $vocalProfile,
                    album.tier = $tier,
                    album.energy = $energy,
                    album.moodIntensity = $moodIntensity,
                    album.accessibility = $accessibility,
                    album.whyItMatters = $whyItMatters,
                    album.editorialNote = $editorialNote,
                    album.recommendedIf = $recommendedIf,
                    album.avoidIf = $avoidIf,
                    album.albumContext = $albumContext,
                    album.captionEssence = $captionEssence,
                    album.releaseDate = CASE
                        WHEN album.releaseDate IS NULL OR trim(album.releaseDate) = '' THEN toString($releaseYear)
                        ELSE album.releaseDate
                    END
                RETURN album.id AS id,
                       album.spotifyAlbumId AS spotifyAlbumId,
                       album.logNumber AS logNumber,
                       album.name AS name
                """)
                .bind(albumData.spotifyAlbumId()).to("spotifyAlbumId")
                .bind(albumData.logNumber()).to("logNumber")
                .bind(albumData.albumName()).to("albumName")
                .bind(normalizeName(albumData.albumName())).to("normalizedName")
                .bind(albumData.postedAt()).to("postedAt")
                .bind(albumData.instagramPermalink()).to("instagramPermalink")
                .bind(albumData.vocalProfile()).to("vocalProfile")
                .bind(albumData.tier()).to("tier")
                .bind(albumData.energy()).to("energy")
                .bind(albumData.moodIntensity()).to("moodIntensity")
                .bind(albumData.accessibility()).to("accessibility")
                .bind(albumData.whyItMatters()).to("whyItMatters")
                .bind(albumData.editorialNote()).to("editorialNote")
                .bind(albumData.recommendedIf()).to("recommendedIf")
                .bind(albumData.avoidIf()).to("avoidIf")
                .bind(albumData.albumContext()).to("albumContext")
                .bind(albumData.captionEssence()).to("captionEssence")
                .bind(albumData.releaseYear()).to("releaseYear")
                .fetch()
                .one()
                .orElseThrow();

        return AlbumNode.builder()
                .id((String) row.get("id"))
                .spotifyAlbumId((String) row.get("spotifyAlbumId"))
                .logNumber(toInteger(row.get("logNumber")))
                .name((String) row.get("name"))
                .build();
    }

    private void replaceLeaderArtists(String albumId, List<AlbumEditorialMainArtist> mainArtists) {
        neo4jClient.query("""
                MATCH (:Artist)-[relationship:LEADER_OF]->(:Album {id: $albumId})
                DELETE relationship
                """)
                .bind(albumId).to("albumId")
                .run();

        var validArtists = mainArtists == null ? List.<java.util.Map<String, Object>>of() : mainArtists.stream()
                .filter(mainArtist -> mainArtist != null
                        && mainArtist.spotifyArtistId() != null && !mainArtist.spotifyArtistId().isBlank()
                        && mainArtist.name() != null && !mainArtist.name().isBlank())
                .map(mainArtist -> {
                    var values = new LinkedHashMap<String, Object>();
                    values.put("spotifyArtistId", mainArtist.spotifyArtistId().trim());
                    values.put("artistName", mainArtist.name().trim());
                    return values;
                })
                .toList();
        if (validArtists.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (album:Album {id: $albumId})
                UNWIND $artists AS artistData
                MERGE (artist:Artist {spotifyArtistId: artistData.spotifyArtistId})
                ON CREATE SET artist.id = randomUUID()
                SET artist.name = artistData.artistName
                MERGE (artist)-[:LEADER_OF]->(album)
                """)
                .bind(albumId).to("albumId")
                .bind(validArtists).to("artists")
                .run();
    }

    private void replaceNamedRelationships(String albumId, String relationshipType, String label, List<String> values) {
        neo4jClient.query("""
                MATCH (album:Album {id: $albumId})
                OPTIONAL MATCH (album)-[relationship:%s]->(:%s)
                DELETE relationship
                """.formatted(relationshipType, label))
                .bind(albumId).to("albumId")
                .run();

        var validValues = distinctNonBlank(values);
        if (validValues.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (album:Album {id: $albumId})
                UNWIND $values AS value
                MERGE (target:%s {name: value})
                MERGE (album)-[:%s]->(target)
                """.formatted(label, relationshipType))
                .bind(albumId).to("albumId")
                .bind(validValues).to("values")
                .run();
    }

    private void replaceSidemen(String albumId, AlbumEditorialData albumData) {
        neo4jClient.query("""
                MATCH (:Artist)-[relationship:SIDEMAN_ON]->(:Album {id: $albumId})
                DELETE relationship
                """)
                .bind(albumId).to("albumId")
                .run();

        var leaderNames = (albumData.mainArtists() == null ? List.<AlbumEditorialMainArtist>of() : albumData.mainArtists()).stream()
                .map(mainArtist -> normalizeName(mainArtist.name()))
                .collect(java.util.stream.Collectors.toSet());

        var sidemen = (albumData.personnel() == null ? List.<AlbumEditorialPersonnel>of() : albumData.personnel()).stream()
                .filter(personnel -> personnel != null
                        && personnel.name() != null && !personnel.name().isBlank()
                        && personnel.instruments() != null && !personnel.instruments().isEmpty())
                .filter(personnel -> !leaderNames.contains(normalizeName(personnel.name())))
                .map(personnel -> {
                    var values = new LinkedHashMap<String, Object>();
                    values.put("artistName", personnel.name().trim());
                    values.put(
                            "spotifyArtistId",
                            personnel.spotifyArtistId() == null || personnel.spotifyArtistId().isBlank()
                                    ? null
                                    : personnel.spotifyArtistId().trim()
                    );
                    values.put("instruments", distinctNonBlank(personnel.instruments()));
                    return values;
                })
                .toList();
        if (sidemen.isEmpty()) {
            return;
        }

        neo4jClient.query("""
                MATCH (album:Album {id: $albumId})
                UNWIND [sidemanData IN $sidemen WHERE sidemanData.spotifyArtistId IS NOT NULL] AS sidemanData
                MERGE (artist:Artist {spotifyArtistId: sidemanData.spotifyArtistId})
                ON CREATE SET artist.id = randomUUID()
                SET artist.name = sidemanData.artistName
                MERGE (artist)-[relationship:SIDEMAN_ON]->(album)
                SET relationship.instruments = sidemanData.instruments
                """)
                .bind(albumId).to("albumId")
                .bind(sidemen).to("sidemen")
                .run();

        neo4jClient.query("""
                MATCH (album:Album {id: $albumId})
                UNWIND [sidemanData IN $sidemen WHERE sidemanData.spotifyArtistId IS NULL] AS sidemanData
                MERGE (artist:Artist {name: sidemanData.artistName})
                ON CREATE SET artist.id = randomUUID()
                SET artist.name = sidemanData.artistName
                MERGE (artist)-[relationship:SIDEMAN_ON]->(album)
                SET relationship.instruments = sidemanData.instruments
                """)
                .bind(albumId).to("albumId")
                .bind(sidemen).to("sidemen")
                .run();
    }

    private String normalizeName(String value) {
        if (value == null) {
            return null;
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
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
}
