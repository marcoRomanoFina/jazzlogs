package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyTokenService;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotConnectedException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotFoundException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyMissingScopesException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTasteSnapshotSyncException;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyTopUserArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyUserTopTrackDTO;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyTasteSyncService {

    private static final int SNAPSHOT_TOP_ITEMS_LIMIT = 5;

    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyClient spotifyClient;
    private final SpotifyTasteSnapshotRepository spotifyTasteSnapshotRepository;
    private final Clock clock;
    private final SpotifyProperties spotifyProperties;

    @Transactional
    public void syncTasteProfile(UUID userId) {
        var connection = spotifyConnectionRepository.findByUserId(userId)
                .orElseThrow(() -> new SpotifyConnectionNotFoundException(userId));

        if (!connection.isConnected()) {
            throw new SpotifyConnectionNotConnectedException(userId, connection.getStatus());
        }

        if (!connection.hasScope(SpotifyScope.USER_TOP_READ)) {
            throw new SpotifyMissingScopesException(userId, java.util.Set.of(SpotifyScope.USER_TOP_READ));
        }

        try {
            var accessToken = spotifyTokenService.getValidAccessToken(userId, connection.getId());
            var generatedAt = Instant.now(clock);
            var timeRange = resolveConfiguredTimeRange();
            var topArtists = fetchTopArtists(accessToken, timeRange).stream()
                    .limit(SNAPSHOT_TOP_ITEMS_LIMIT)
                    .toList();
            var topTracks = fetchTopTracks(accessToken, timeRange).stream()
                    .limit(SNAPSHOT_TOP_ITEMS_LIMIT)
                    .toList();
            spotifyTasteSnapshotRepository.deleteAllByUserId(userId);
            spotifyTasteSnapshotRepository.save(
                    SpotifyTasteSnapshot.create(
                            userId,
                            connection.getId(),
                            topArtists,
                            topTracks,
                            generatedAt
                    )
            );
        }
        catch (SpotifyConnectionNotFoundException
               | SpotifyConnectionNotConnectedException
               | SpotifyMissingScopesException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            log.error("Spotify taste snapshot sync failed for userId={}", userId, exception);
            throw new SpotifyTasteSnapshotSyncException(
                    "Failed to sync Spotify taste snapshot for user " + userId,
                    exception
            );
        }
    }

    private List<SpotifyTopUserArtistDTO> fetchTopArtists(
            String accessToken,
            SpotifyTimeRange timeRange
    ) {
        return spotifyClient.getTopArtists(accessToken, timeRange, spotifyProperties.taste().topArtistsLimit());
    }

    private List<SpotifyUserTopTrackDTO> fetchTopTracks(
            String accessToken,
            SpotifyTimeRange timeRange
    ) {
        return spotifyClient.getTopTracks(accessToken, timeRange, spotifyProperties.taste().topTracksLimit());
    }

    private SpotifyTimeRange resolveConfiguredTimeRange() {
        if (spotifyProperties.taste().timeRange() == null) {
            throw new IllegalStateException("Spotify taste time range is not configured");
        }
        return spotifyProperties.taste().timeRange();
    }
}
