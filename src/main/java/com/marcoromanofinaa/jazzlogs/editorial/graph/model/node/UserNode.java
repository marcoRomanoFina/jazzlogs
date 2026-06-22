package com.marcoromanofinaa.jazzlogs.editorial.graph.model.node;

import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;
import org.springframework.data.neo4j.core.schema.Relationship;

@Node("User")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserNode {

    @Id
    private String userId;
    private String displayName;
    private String jazzExperienceLevel;
    private String discoveryMode;
    private Boolean likesVocals;

    @Relationship(type = "LIKES_ARTIST", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<ArtistNode> favoriteArtists = new LinkedHashSet<>();

    @Relationship(type = "LIKES_INSTRUMENT", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<InstrumentNode> favoriteInstruments = new LinkedHashSet<>();

    @Relationship(type = "LIKES_STYLE", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<StyleNode> preferredStyles = new LinkedHashSet<>();

    @Relationship(type = "PREFERS_MOOD", direction = Relationship.Direction.OUTGOING)
    @Builder.Default
    private Set<MoodNode> preferredMoods = new LinkedHashSet<>();

    private String preferredTempoFeel;
}
