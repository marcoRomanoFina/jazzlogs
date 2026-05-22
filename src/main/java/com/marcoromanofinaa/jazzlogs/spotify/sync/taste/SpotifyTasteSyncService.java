package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnection;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpotifyTasteSyncService {

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
            var topArtists = buildTopArtists(accessToken);
            var topTracks = buildTopTracks(accessToken);

            var existingSnapshot = spotifyTasteSnapshotRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
            if (existingSnapshot.isPresent()) {
                existingSnapshot.get().replaceSnapshot(connection.getId(), topArtists, topTracks, generatedAt);
                return;
            }

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
            throw new SpotifyTasteSnapshotSyncException(
                    "Failed to sync Spotify taste snapshot for user " + userId,
                    exception
            );
        }
    }

    private Map<SpotifyTimeRange, List<SpotifyTopUserArtistDTO>> buildTopArtists(String accessToken) {
        var topArtists = new EnumMap<SpotifyTimeRange, List<SpotifyTopUserArtistDTO>>(SpotifyTimeRange.class);
        for (var timeRange : SpotifyTimeRange.values()) {
            topArtists.put(
                    timeRange,
                    spotifyClient.getTopArtists(accessToken, timeRange, spotifyProperties.taste().topArtistsLimit())
            );
        }
        return topArtists;
    }

    private Map<SpotifyTimeRange, List<SpotifyUserTopTrackDTO>> buildTopTracks(String accessToken) {
        var topTracks = new EnumMap<SpotifyTimeRange, List<SpotifyUserTopTrackDTO>>(SpotifyTimeRange.class);
        for (var timeRange : SpotifyTimeRange.values()) {
            topTracks.put(
                    timeRange,
                    spotifyClient.getTopTracks(accessToken, timeRange, spotifyProperties.taste().topTracksLimit())
            );
        }
        return topTracks;
    }
}
