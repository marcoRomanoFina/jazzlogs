package com.marcoromanofinaa.jazzlogs.spotify.playlist;

import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SpotifyPlaylistSyncAsyncService {

    // El sync de arranque es async para que la API pueda levantar aunque Spotify esté lento o caído.
    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;

    @Async
    public void syncConfiguredPlaylistAsync() {
        try {
            var syncedCount = spotifyPlaylistSyncService.syncConfiguredPlaylist();
            log.info("Async Spotify playlist sync completed with {} items", syncedCount);
        }
        catch (SpotifyException exception) {
            log.warn("Async Spotify playlist sync skipped: {}", exception.getMessage());
        }
        catch (Exception exception) {
            log.error("Async Spotify playlist sync failed", exception);
        }
    }
}
