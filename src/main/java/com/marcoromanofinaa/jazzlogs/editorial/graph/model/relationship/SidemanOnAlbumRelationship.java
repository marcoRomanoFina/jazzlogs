package com.marcoromanofinaa.jazzlogs.editorial.graph.model.relationship;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.AlbumNode;
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
import java.util.List;

@RelationshipProperties
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SidemanOnAlbumRelationship {

    @Id
    @GeneratedValue
    private String id;

    private List<String> instruments;

    @TargetNode
    private AlbumNode album;
}
