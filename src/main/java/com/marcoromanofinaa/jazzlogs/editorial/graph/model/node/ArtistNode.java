package com.marcoromanofinaa.jazzlogs.editorial.graph.model.node;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.SidemanOnAlbumRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.PerformedOnTrackRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.support.UuidStringGenerator;
import java.util.List;
import java.util.LinkedHashSet;
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

@Node("Artist")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ArtistNode {

    @Id
    @GeneratedValue(generatorClass = UuidStringGenerator.class)
    private String id;

    private String spotifyArtistId;

    private String name;
    private String normalizedName;
    private String spotifyUrl;
    private String signatureSound;
    private String artistContext;
    private String jazzlogsTake;
    private String avoidIf;
    private String editorialImportance;
    private List<Integer> logAppearances;
    private List<Double> embedding;

    @Relationship(type = "PLAYS_INSTRUMENT", direction = Relationship.Direction.OUTGOING)
    private InstrumentNode primaryInstrument;

    @Relationship(type = "HAS_STYLE", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<StyleNode> mainStyles = new LinkedHashSet<>();

    @Relationship(type = "PERFECT_FOR", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ContextNode> bestForContexts = new LinkedHashSet<>();

    @Relationship(type = "SIMILAR_TO", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ArtistNode> relatedArtists = new LinkedHashSet<>();

    @Relationship(type = "LEADER_OF", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<AlbumNode> leaderOfAlbums = new LinkedHashSet<>();

    @Relationship(type = "SIDEMAN_ON", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<SidemanOnAlbumRelationship> sidemanOnAlbums = new LinkedHashSet<>();

    @Relationship(type = "PERFORMED_ON", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<PerformedOnTrackRelationship> performedOnTracks = new LinkedHashSet<>();
}
