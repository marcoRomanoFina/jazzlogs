package com.marcoromanofinaa.jazzlogs.curation.core;

public record CurationUpsertResult(
        CurationElementType type,
        String identifier,
        int upsertedCount
) {
}
