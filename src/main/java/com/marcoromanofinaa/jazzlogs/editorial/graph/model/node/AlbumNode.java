package com.marcoromanofinaa.jazzlogs.editorial.graph.model.node;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.AlbumSidemanRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.ContainsTrackRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.support.UuidStringGenerator;
import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Album")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlbumNode {

    @Id
    @GeneratedValue(generatorClass = UuidStringGenerator.class)
    private String id;

    private String spotifyAlbumId;

    private Integer logNumber;

    private String name;
    private String normalizedName;
    private String releaseDate;
    private Integer totalTracks;
    private String imageUrl;
    private String spotifyUrl;
    private String vocalProfile;
    private String energy;
    private String moodIntensity;
    private String tier;
    private String accessibility;
    private LocalDate postedAt;
    private String whyItMatters;
    private String editorialNote;
    private String recommendedIf;
    private String avoidIf;
    private String albumContext;
    private String captionEssence;
    private String instagramPermalink;
    private List<Double> embedding;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ContainsTrackRelationship> tracks = new LinkedHashSet<>();

    @Relationship(type = "LEADER_OF", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<ArtistNode> leaderArtists = new LinkedHashSet<>();

    @Relationship(type = "SIDEMAN_ON", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<AlbumSidemanRelationship> sidemen = new LinkedHashSet<>();

    @Relationship(type = "ENTRY_POINT_TO", direction = Relationship.Direction.OUTGOING)
    private ArtistNode entryPointToArtist;

    @Relationship(type = "BELONGS_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<StyleNode> styles = new LinkedHashSet<>();

    @Relationship(type = "EVOKES_MOOD", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<MoodNode> moods = new LinkedHashSet<>();

    @Relationship(type = "PERFECT_FOR", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ContextNode> contexts = new LinkedHashSet<>();
}
