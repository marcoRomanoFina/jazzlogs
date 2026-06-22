package com.marcoromanofinaa.jazzlogs.editorial.graph.model.node;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship.TrackPerformedByArtistRelationship;
import com.marcoromanofinaa.jazzlogs.editorial.graph.support.UuidStringGenerator;
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
import org.springframework.data.neo4j.core.schema.Property;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("Track")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackNode {

    @Id
    @GeneratedValue(generatorClass = UuidStringGenerator.class)
    private String id;

    @Property("spotify_track_id")
    private String spotifyTrackId;

    private Integer logNumber;
    private String name;
    private String normalizedName;
    private Integer durationMs;
    private String spotifyUrl;
    private Boolean isInstrumental;
    private Boolean isStandout;
    private String tier;
    private String energy;
    private String editorialNote;
    private String whyItHits;
    private String bestMoment;
    private String accessibility;
    private String vocalProfile;
    private String moodIntensity;
    private String tempoFeel;
    private String compositionType;
    private String recommendedIf;
    private String avoidIf;
    private String vocalStyle;
    private List<Double> embedding;

    @Relationship(type = "CONTAINS", direction = Relationship.Direction.INCOMING)
    private AlbumNode album;

    @Relationship(type = "PERFORMED_ON", direction = Relationship.Direction.INCOMING)
    @Builder.Default
    private Set<TrackPerformedByArtistRelationship> performedByArtists = new LinkedHashSet<>();

    @Relationship(type = "EVOKES_MOOD", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<MoodNode> moods = new LinkedHashSet<>();

    @Relationship(type = "PERFECT_FOR", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ContextNode> contexts = new LinkedHashSet<>();

    @Relationship(type = "HAS_RHYTHM", direction = Relationship.Direction.OUTGOING)
    private RhythmNode rhythm;

    @Relationship(type = "FEATURES_INSTRUMENT", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<InstrumentNode> instrumentFocus = new LinkedHashSet<>();

    @Relationship(type = "ENTRY_POINT_TO", direction = Relationship.Direction.OUTGOING)
    private ArtistNode entryPointToArtist;
}
