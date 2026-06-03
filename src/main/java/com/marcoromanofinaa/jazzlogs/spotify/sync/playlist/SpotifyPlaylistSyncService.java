package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;
import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnection;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionStatus;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyTokenService;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotConnectedException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotFoundException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyMissingScopesException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyPlaylistSyncException;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyPlaylistSyncService {

    private static final Set<SpotifyScope> REQUIRED_SCOPES = Set.of(
            SpotifyScope.PLAYLIST_READ_PRIVATE
    );

    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyTokenService spotifyTokenService;
    private final SpotifyClient spotifyClient;
    private final SpotifyPlaylistImportService spotifyPlaylistImportService;
    private final SpotifyProperties spotifyProperties;

    @Transactional
    public void syncOfficialPlaylist(UUID adminUserId) {
        try {
            var officialPlaylistId = requireOfficialPlaylistId();
            var connection = spotifyConnectionRepository.findByUserIdAndStatus(adminUserId, SpotifyConnectionStatus.CONNECTED)
                    .orElseThrow(() -> new SpotifyConnectionNotFoundException(adminUserId));

            validateConnection(connection);

            var accessToken = spotifyTokenService.getValidAccessToken(adminUserId, connection.getId());
            var tracks = spotifyClient.getPlaylistTracks(accessToken, officialPlaylistId);
            spotifyPlaylistImportService.importPlaylistTracks(tracks);
            log.info(
                    "Completed Spotify official playlist sync for adminUserId={} with {} tracks fetched",
                    adminUserId,
                    tracks.size()
            );
        }
        catch (SpotifyConnectionNotFoundException
               | SpotifyConnectionNotConnectedException
               | SpotifyMissingScopesException exception) {
            throw exception;
        }
        catch (RuntimeException exception) {
            log.error(
                    "Spotify official playlist sync failed for adminUserId={} and playlistId={}",
                    adminUserId,
                    spotifyProperties.sync().officialPlaylistId(),
                    exception
            );
            throw new SpotifyPlaylistSyncException(
                    "Failed to sync official Spotify playlist for user " + adminUserId,
                    exception
            );
        }
    }

    private void validateConnection(SpotifyConnection connection) {
        if (!connection.isConnected()) {
            throw new SpotifyConnectionNotConnectedException(connection.getUserId(), connection.getStatus());
        }

        if (!connection.hasScopes(REQUIRED_SCOPES)) {
            throw new SpotifyMissingScopesException(connection.getUserId(), REQUIRED_SCOPES);
        }
    }

    private String requireOfficialPlaylistId() {
        return requireConfiguredValue(
                spotifyProperties.sync().officialPlaylistId(),
                "Spotify official playlist ID is not configured"
        );
    }

    private String requireConfiguredValue(String value, String errorMessage) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(errorMessage);
        }
        return value;
    }
}
