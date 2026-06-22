package com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.TrackNode;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.RelationshipProperties;
import org.springframework.data.neo4j.core.schema.TargetNode;

@RelationshipProperties
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContainsTrackRelationship {

    @Id
    @GeneratedValue
    private String id;

    private Integer trackNumber;
    private String trackRole;

    @TargetNode
    private TrackNode track;
}
