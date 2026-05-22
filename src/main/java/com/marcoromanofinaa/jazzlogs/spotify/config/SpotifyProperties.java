package com.marcoromanofinaa.jazzlogs.spotify.config;

import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.SpotifyTimeRange;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.spotify")
public record SpotifyProperties(
        OAuth oauth,
        Api api,
        Sync sync,
        Security security,
        Taste taste
) {

    public record OAuth(
            String clientId,
            String clientSecret,
            String redirectUri,
            String tokenUrl,
            Duration stateTtl,
            Duration cleanupRetention,
            Redirect redirect
    ) {
    }

    public record Redirect(
            String successUri,
            String errorUri
    ) {
    }

    public record Api(
            String baseUrl
    ) {
    }

    public record Sync(
            String officialPlaylistId,
            String officialOwnerSpotifyUserId,
            String market,
            boolean enabled,
            String cron,
            String zone,
            boolean syncOnStartup,
            int pageSize
    ) {
    }

    public record Security(
            String tokenEncryptionSecret
    ) {
    }

    public record Taste(
            SpotifyTimeRange timeRange,
            int topArtistsLimit,
            int topTracksLimit
    ) {
    }
}
