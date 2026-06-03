package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jazzlogs.spotify.sync", name = "enabled", havingValue = "true")
public class SpotifyPlaylistDailySyncJob {

    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;

    @Scheduled(
            cron = "${jazzlogs.spotify.sync.cron:0 0 0 * * *}",
            zone = "${jazzlogs.spotify.sync.zone:America/Argentina/Buenos_Aires}"
    )
    public void syncOfficialPlaylistDaily() {
        try {
            var connection = spotifyConnectionRepository.findFirstByStatusOrderByConnectedAtAsc(
                            SpotifyConnectionStatus.CONNECTED
                    )
                    .orElse(null);

            if (connection == null) {
                log.info(
                        "Skipping Spotify daily playlist sync because no connected Spotify user was found"
                );
                return;
            }

            spotifyPlaylistSyncService.syncOfficialPlaylist(connection.getUserId());
            log.info("Spotify daily playlist sync job completed");
        }
        catch (Exception exception) {
            log.error("Spotify daily playlist sync job failed", exception);
        }
    }
}
