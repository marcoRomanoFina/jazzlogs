package com.marcoromanofinaa.jazzlogs.ingestion.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.ingestion.json")
public record AlbumLogIngestionProperties(
        boolean enabled,
        String path
) {
}
