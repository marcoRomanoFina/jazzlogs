package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
public class SpotifyOAuthStateCleanupJob {

    private final SpotifyOAuthStateRepository spotifyOAuthStateRepository;
    private final SpotifyProperties spotifyProperties;

    @Transactional
    @Scheduled(cron = "0 */15 * * * *")
    public void cleanExpiredStates() {
        var now = Instant.now();
        var expiredCount = spotifyOAuthStateRepository.expirePendingStates(
                SpotifyOAuthStateStatus.PENDING,
                SpotifyOAuthStateStatus.EXPIRED,
                now
        );
        var deletedCount = spotifyOAuthStateRepository.deleteAllByStatusInAndCreatedAtBefore(
                List.of(SpotifyOAuthStateStatus.CONSUMED, SpotifyOAuthStateStatus.EXPIRED),
                now.minus(spotifyProperties.oauth().cleanupRetention())
        );

        if (expiredCount > 0 || deletedCount > 0) {
            log.info(
                    "Spotify OAuth state cleanup expired={} deleted={}",
                    expiredCount,
                    deletedCount
            );
        }
    }
}
