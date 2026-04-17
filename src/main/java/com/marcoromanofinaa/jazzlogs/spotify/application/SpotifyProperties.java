package com.marcoromanofinaa.jazzlogs.spotify.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.spotify")
public record SpotifyProperties(
        String clientId,
        String clientSecret,
        String redirectUri,
        String playlistId,
        String market,
        boolean syncEnabled,
        String syncCron,
        String syncZone,
        boolean syncOnStartup
) {
}
