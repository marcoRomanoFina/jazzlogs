package com.marcoromanofinaa.jazzlogs.curation.admin;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.admin")
public record AdminApiProperties(
        String apiKey
) {
}
