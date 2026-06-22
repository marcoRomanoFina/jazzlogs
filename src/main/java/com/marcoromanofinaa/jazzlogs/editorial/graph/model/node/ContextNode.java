package com.marcoromanofinaa.jazzlogs.editorial.graph.model.node;

import com.marcoromanofinaa.jazzlogs.editorial.graph.support.UuidStringGenerator;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.neo4j.core.schema.GeneratedValue;
import org.springframework.data.neo4j.core.schema.Id;
import org.springframework.data.neo4j.core.schema.Node;

@Node("Context")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ContextNode {

    @Id
    @GeneratedValue(generatorClass = UuidStringGenerator.class)
    private String id;

    private String name;
}
