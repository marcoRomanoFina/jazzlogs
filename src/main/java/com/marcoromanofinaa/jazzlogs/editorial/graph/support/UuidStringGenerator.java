package com.marcoromanofinaa.jazzlogs.editorial.graph.support;

import java.util.UUID;
import org.springframework.data.neo4j.core.schema.IdGenerator;

public class UuidStringGenerator implements IdGenerator<String> {

    @Override
    public String generateId(String primaryLabel, Object entity) {
        return UUID.randomUUID().toString();
    }
}
