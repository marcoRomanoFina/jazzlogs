package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnection;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.SpotifyTokenService;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyMissingScopesException;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyTopUserArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyUserTopTrackDTO;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpotifyTasteSyncServiceTest {

    @Mock
    private SpotifyConnectionRepository spotifyConnectionRepository;

    @Mock
    private SpotifyTokenService spotifyTokenService;

    @Mock
    private SpotifyClient spotifyClient;

    @Mock
    private SpotifyTasteSnapshotRepository spotifyTasteSnapshotRepository;

    @Test
    void syncTasteProfileBuildsSnapshotForConfiguredTimeRange() {
        var now = Instant.parse("2026-05-22T12:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var service = new SpotifyTasteSyncService(
                spotifyConnectionRepository,
                spotifyTokenService,
                spotifyClient,
                spotifyTasteSnapshotRepository,
                clock,
                spotifyProperties()
        );
        var userId = UUID.randomUUID();
        var connection = SpotifyConnection.create(
                userId,
                "spotify-user",
                "Display Name",
                "AR",
                "premium",
                "enc-access",
                "enc-refresh",
                "Bearer",
                "user-top-read",
                now.plusSeconds(3600)
        );
        ReflectionTestUtils.setField(connection, "id", UUID.randomUUID());
        var configuredTimeRange = SpotifyTimeRange.MEDIUM_TERM;

        when(spotifyConnectionRepository.findByUserId(userId)).thenReturn(Optional.of(connection));
        when(spotifyTokenService.getValidAccessToken(userId, connection.getId())).thenReturn("access-token");
        when(spotifyClient.getTopArtists("access-token", configuredTimeRange, 7))
                .thenReturn(List.of(new SpotifyTopUserArtistDTO("artist-" + configuredTimeRange.name(), "Artist " + configuredTimeRange.name())));
        when(spotifyClient.getTopTracks("access-token", configuredTimeRange, 9))
                .thenReturn(List.of(new SpotifyUserTopTrackDTO("track-" + configuredTimeRange.name(), "Track " + configuredTimeRange.name(), List.of("Artist"), "Album")));
        when(spotifyTasteSnapshotRepository.findTopByUserIdOrderByGeneratedAtDesc(userId)).thenReturn(Optional.empty());

        service.syncTasteProfile(userId);

        var snapshotCaptor = ArgumentCaptor.forClass(SpotifyTasteSnapshot.class);
        verify(spotifyTasteSnapshotRepository).save(snapshotCaptor.capture());
        assertThat(snapshotCaptor.getValue().getUserId()).isEqualTo(userId);
        assertThat(snapshotCaptor.getValue().getSpotifyConnectionId()).isEqualTo(connection.getId());
        assertThat(snapshotCaptor.getValue().getGeneratedAt()).isEqualTo(now);
        assertThat(snapshotCaptor.getValue().getTopArtists())
                .extracting(SpotifyTopUserArtistDTO::spotifyArtistId)
                .containsExactly("artist-" + configuredTimeRange.name());
        assertThat(snapshotCaptor.getValue().getTopTracks())
                .extracting(SpotifyUserTopTrackDTO::spotifyTrackId)
                .containsExactly("track-" + configuredTimeRange.name());
    }

    @Test
    void syncTasteProfileRejectsConnectionWithoutRequiredScope() {
        var service = new SpotifyTasteSyncService(
                spotifyConnectionRepository,
                spotifyTokenService,
                spotifyClient,
                spotifyTasteSnapshotRepository,
                Clock.systemUTC(),
                spotifyProperties()
        );
        var userId = UUID.randomUUID();
        var connection = SpotifyConnection.create(
                userId,
                "spotify-user",
                "Display Name",
                "AR",
                "premium",
                "enc-access",
                "enc-refresh",
                "Bearer",
                "playlist-read-private",
                Instant.now().plusSeconds(3600)
        );

        when(spotifyConnectionRepository.findByUserId(userId)).thenReturn(Optional.of(connection));

        assertThatThrownBy(() -> service.syncTasteProfile(userId))
                .isInstanceOf(SpotifyMissingScopesException.class);
    }

    private SpotifyProperties spotifyProperties() {
        return new SpotifyProperties(
                new SpotifyProperties.OAuth(
                        "client-id",
                        "client-secret",
                        "https://callback",
                        "https://token",
                        java.time.Duration.ofMinutes(10),
                        java.time.Duration.ofDays(1),
                        new SpotifyProperties.Redirect("https://frontend/success", "https://frontend/error")
                ),
                new SpotifyProperties.Api("https://api.spotify.test"),
                new SpotifyProperties.Sync("playlist-id", "owner-id", "AR", true, "0 0 * * * *", "UTC", false, 50),
                new SpotifyProperties.Security("secret"),
                new SpotifyProperties.Taste(SpotifyTimeRange.MEDIUM_TERM, 7, 9)
        );
    }
}
