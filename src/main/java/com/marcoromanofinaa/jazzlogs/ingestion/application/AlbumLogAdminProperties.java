package com.marcoromanofinaa.jazzlogs.ingestion.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.admin")
public record AlbumLogAdminProperties(
        String apiKey
) {
}
