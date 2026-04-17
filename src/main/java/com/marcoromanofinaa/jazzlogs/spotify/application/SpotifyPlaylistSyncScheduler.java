package com.marcoromanofinaa.jazzlogs.spotify.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jazzlogs.spotify.sync", name = "enabled", havingValue = "true")
public class SpotifyPlaylistSyncScheduler {

    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;

    @Scheduled(
            cron = "${jazzlogs.spotify.sync.cron}",
            zone = "${jazzlogs.spotify.sync.zone:America/Argentina/Buenos_Aires}"
    )
    public void syncPlaylist() {
        try {
            var syncedCount = spotifyPlaylistSyncService.syncConfiguredPlaylist();
            log.info("Scheduled Spotify playlist sync completed with {} items", syncedCount);
        }
        catch (SpotifyException exception) {
            log.warn("Scheduled Spotify playlist sync skipped: {}", exception.getMessage());
        }
        catch (Exception exception) {
            log.error("Scheduled Spotify playlist sync failed", exception);
        }
    }
}
