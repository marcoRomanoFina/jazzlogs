package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnection;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionStatus;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyTokenService;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.exception.InvalidOfficialSpotifyOwnerException;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpotifyPlaylistSyncServiceTest {

    @Mock
    private SpotifyConnectionRepository spotifyConnectionRepository;

    @Mock
    private SpotifyTokenService spotifyTokenService;

    @Mock
    private SpotifyClient spotifyClient;

    @Mock
    private SpotifyPlaylistImportService spotifyPlaylistImportService;

    @Test
    void syncOfficialPlaylistFetchesTracksAndDelegatesImport() {
        var service = new SpotifyPlaylistSyncService(
                spotifyConnectionRepository,
                spotifyTokenService,
                spotifyClient,
                spotifyPlaylistImportService,
                spotifyProperties()
        );
        var adminUserId = UUID.randomUUID();
        var connection = SpotifyConnection.create(
                adminUserId,
                "official-owner",
                "Display Name",
                "AR",
                "premium",
                "enc-access",
                "enc-refresh",
                "Bearer",
                "playlist-read-private",
                Instant.now().plusSeconds(3600)
        );
        ReflectionTestUtils.setField(connection, "id", UUID.randomUUID());
        var tracks = List.of(
                new SpotifyPlaylistTrackDTO(
                        "track-1",
                        "Blue in Green",
                        List.of(new SpotifyArtistDTO("artist-1", "Miles Davis", "https://spotify/artist-1")),
                        new SpotifyAlbumDTO("album-1", "Kind of Blue", List.of(), "1959-08-17", 5, "https://img/1", "https://spotify/album-1"),
                        327000,
                        3,
                        "https://spotify/track-1"
                )
        );

        when(spotifyConnectionRepository.findByUserIdAndStatus(adminUserId, SpotifyConnectionStatus.CONNECTED))
                .thenReturn(Optional.of(connection));
        when(spotifyTokenService.getValidAccessToken(adminUserId, connection.getId())).thenReturn("access-token");
        when(spotifyClient.getPlaylistTracks("access-token", "playlist-id")).thenReturn(tracks);

        service.syncOfficialPlaylist(adminUserId);

        verify(spotifyPlaylistImportService).importPlaylistTracks(tracks);
    }

    @Test
    void syncOfficialPlaylistRejectsConnectionForUnexpectedSpotifyOwner() {
        var service = new SpotifyPlaylistSyncService(
                spotifyConnectionRepository,
                spotifyTokenService,
                spotifyClient,
                spotifyPlaylistImportService,
                spotifyProperties()
        );
        var adminUserId = UUID.randomUUID();
        var connection = SpotifyConnection.create(
                adminUserId,
                "another-owner",
                "Display Name",
                "AR",
                "premium",
                "enc-access",
                "enc-refresh",
                "Bearer",
                "playlist-read-private",
                Instant.now().plusSeconds(3600)
        );

        when(spotifyConnectionRepository.findByUserIdAndStatus(adminUserId, SpotifyConnectionStatus.CONNECTED))
                .thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.syncOfficialPlaylist(adminUserId))
                .isInstanceOf(InvalidOfficialSpotifyOwnerException.class);
    }

    private SpotifyProperties spotifyProperties() {
        return new SpotifyProperties(
                new SpotifyProperties.OAuth(
                        "client-id",
                        "client-secret",
                        "https://callback",
                        "https://token",
                        Duration.ofMinutes(10),
                        Duration.ofDays(1),
                        new SpotifyProperties.Redirect("https://frontend/success", "https://frontend/error")
                ),
                new SpotifyProperties.Api("https://api.spotify.test"),
                new SpotifyProperties.Sync("playlist-id", "official-owner", "AR", true, "0 0 * * * *", "UTC", false, 50),
                new SpotifyProperties.Security("secret"),
                new SpotifyProperties.Taste(com.marcoromanofinaa.jazzlogs.spotify.sync.taste.SpotifyTimeRange.MEDIUM_TERM, 10, 10)
        );
    }
}
