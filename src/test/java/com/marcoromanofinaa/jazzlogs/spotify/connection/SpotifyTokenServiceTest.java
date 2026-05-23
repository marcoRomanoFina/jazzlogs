package com.marcoromanofinaa.jazzlogs.spotify.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenClient;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenResponseDTO;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.TokenEncryptionService;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenExchangeException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenRefreshException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SpotifyTokenServiceTest {

    @Mock
    private SpotifyConnectionRepository spotifyConnectionRepository;

    @Mock
    private SpotifyTokenClient spotifyTokenClient;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Test
    void getValidAccessTokenReturnsDecryptedTokenWhenConnectionIsStillValid() {
        var now = Instant.parse("2026-05-22T12:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var service = new SpotifyTokenService(
                spotifyConnectionRepository,
                spotifyTokenClient,
                tokenEncryptionService,
                clock
        );
        var userId = UUID.randomUUID();
        var connectionId = UUID.randomUUID();
        var connection = connectedConnection(userId, Instant.parse("2026-05-22T13:00:00Z"));
        ReflectionTestUtils.setField(connection, "id", connectionId);

        when(spotifyConnectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionService.decrypt("enc-access")).thenReturn("plain-access");

        var accessToken = service.getValidAccessToken(userId, connectionId);

        assertThat(accessToken).isEqualTo("plain-access");
        verify(spotifyConnectionRepository).save(connection);
    }

    @Test
    void getValidAccessTokenRefreshesAndPersistsNewTokensWhenExpired() {
        var now = Instant.parse("2026-05-22T12:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var service = new SpotifyTokenService(
                spotifyConnectionRepository,
                spotifyTokenClient,
                tokenEncryptionService,
                clock
        );
        var userId = UUID.randomUUID();
        var connectionId = UUID.randomUUID();
        var connection = connectedConnection(userId, Instant.parse("2026-05-22T11:00:00Z"));
        ReflectionTestUtils.setField(connection, "id", connectionId);

        when(spotifyConnectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionService.decrypt("enc-refresh")).thenReturn("refresh-plain");
        when(spotifyTokenClient.refreshAccessToken("refresh-plain")).thenReturn(
                new SpotifyTokenResponseDTO("new-access", "Bearer", "user-top-read", 3600, "new-refresh")
        );
        when(tokenEncryptionService.encrypt("new-access")).thenReturn("enc-new-access");
        when(tokenEncryptionService.encrypt("new-refresh")).thenReturn("enc-new-refresh");

        var accessToken = service.getValidAccessToken(userId, connectionId);

        assertThat(accessToken).isEqualTo("new-access");
        assertThat(connection.getEncryptedAccessToken()).isEqualTo("enc-new-access");
        assertThat(connection.getEncryptedRefreshToken()).isEqualTo("enc-new-refresh");
        assertThat(connection.getTokenType()).isEqualTo("Bearer");
        assertThat(connection.getGrantedScopes()).isEqualTo("user-top-read");
        verify(spotifyConnectionRepository).save(connection);
    }

    @Test
    void getValidAccessTokenMarksConnectionAsErrorWhenRefreshIsRejected() {
        var now = Instant.parse("2026-05-22T12:00:00Z");
        var clock = Clock.fixed(now, ZoneOffset.UTC);
        var service = new SpotifyTokenService(
                spotifyConnectionRepository,
                spotifyTokenClient,
                tokenEncryptionService,
                clock
        );
        var userId = UUID.randomUUID();
        var connectionId = UUID.randomUUID();
        var connection = connectedConnection(userId, Instant.parse("2026-05-22T11:00:00Z"));
        ReflectionTestUtils.setField(connection, "id", connectionId);

        when(spotifyConnectionRepository.findById(connectionId)).thenReturn(Optional.of(connection));
        when(tokenEncryptionService.decrypt("enc-refresh")).thenReturn("refresh-plain");
        when(spotifyTokenClient.refreshAccessToken("refresh-plain"))
                .thenThrow(new SpotifyTokenExchangeException("invalid refresh token"));

        assertThatThrownBy(() -> service.getValidAccessToken(userId, connectionId))
                .isInstanceOf(SpotifyTokenRefreshException.class)
                .hasMessageContaining(connectionId.toString());

        assertThat(connection.getStatus()).isEqualTo(SpotifyConnectionStatus.ERROR);
        verify(spotifyConnectionRepository).save(connection);
    }

    private SpotifyConnection connectedConnection(UUID userId, Instant expiresAt) {
        return SpotifyConnection.create(
                userId,
                "spotify-user",
                "Display Name",
                "AR",
                "premium",
                "enc-access",
                "enc-refresh",
                "Bearer",
                "user-top-read",
                expiresAt
        );
    }
}
