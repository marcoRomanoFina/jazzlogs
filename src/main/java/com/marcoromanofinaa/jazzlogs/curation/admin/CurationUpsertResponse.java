package com.marcoromanofinaa.jazzlogs.curation.admin;

public record CurationUpsertResponse(
        String resourceType,
        String identifier,
        int upsertedCount
) {
}
