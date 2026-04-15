package com.marcoromanofinaa.jazzlogs.ingestion.web;

public record AlbumLogIngestionResponse(
        int importedCount,
        String source
) {
}
