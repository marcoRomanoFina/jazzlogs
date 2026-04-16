package com.marcoromanofinaa.jazzlogs.spotify.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jazzlogs.spotify.sync", name = "on-startup", havingValue = "true")
public class SpotifyPlaylistSyncStartupRunner implements ApplicationRunner {

    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;

    @Override
    public void run(ApplicationArguments args) {
        try {
            var syncedCount = spotifyPlaylistSyncService.syncConfiguredPlaylist();
            log.info("Startup Spotify playlist sync completed with {} items", syncedCount);
        }
        catch (SpotifyException exception) {
            log.warn("Startup Spotify playlist sync skipped: {}", exception.getMessage());
        }
        catch (Exception exception) {
            log.error("Startup Spotify playlist sync failed", exception);
        }
    }
}
