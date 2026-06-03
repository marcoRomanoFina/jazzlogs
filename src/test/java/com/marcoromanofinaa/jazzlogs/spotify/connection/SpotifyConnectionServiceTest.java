package com.marcoromanofinaa.jazzlogs.spotify.connection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenClient;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenResponseDTO;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyUserProfileDTO;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.OAuthStateGenerator;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyAuthorizationUrlBuilder;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthState;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthStateRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthStateStatus;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.TokenEncryptionService;
import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyAccountAlreadyLinkedException;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.exception.ExpiredSpotifyOAuthStateException;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotifyConnectionServiceTest {

    @Mock
    private OAuthStateGenerator oAuthStateGenerator;

    @Mock
    private SpotifyConnectionRepository spotifyConnectionRepository;

    @Mock
    private SpotifyAuthorizationUrlBuilder spotifyAuthorizationUrlBuilder;

    @Mock
    private SpotifyTokenClient spotifyTokenClient;

    @Mock
    private TokenEncryptionService tokenEncryptionService;

    @Mock
    private SpotifyOAuthStateRepository spotifyOAuthStateRepository;

    @Mock
    private SpotifyClient spotifyClient;

    @InjectMocks
    private SpotifyConnectionService spotifyConnectionService;

    @Test
    void createAuthorizationUrlPersistsPendingStateAndReturnsBuiltUrl() {
        var userId = UUID.randomUUID();
        var scopes = Set.of(SpotifyScope.USER_TOP_READ, SpotifyScope.USER_READ_EMAIL);
        var properties = spotifyProperties();
        spotifyConnectionService = new SpotifyConnectionService(
                oAuthStateGenerator,
                spotifyConnectionRepository,
                spotifyAuthorizationUrlBuilder,
                spotifyTokenClient,
                tokenEncryptionService,
                spotifyOAuthStateRepository,
                spotifyClient,
                properties
        );

        when(oAuthStateGenerator.generate()).thenReturn("state-123");
        when(spotifyAuthorizationUrlBuilder.build("state-123", scopes)).thenReturn("https://spotify.test/auth");
        when(spotifyOAuthStateRepository.save(any(SpotifyOAuthState.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = spotifyConnectionService.createAuthorizationUrl(userId, scopes);

        assertThat(response).isEqualTo(new SpotifyAuthorizationUrlDTO("https://spotify.test/auth"));
        var captor = ArgumentCaptor.forClass(SpotifyOAuthState.class);
        verify(spotifyOAuthStateRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(userId);
        assertThat(captor.getValue().getState()).isEqualTo("state-123");
        assertThat(captor.getValue().getStatus()).isEqualTo(SpotifyOAuthStateStatus.PENDING);
        assertThat(captor.getValue().getRequestedScopes()).containsExactlyInAnyOrder(
                SpotifyScope.USER_READ_EMAIL.value(),
                SpotifyScope.USER_TOP_READ.value()
        );
    }

    @Test
    void handleCallbackCreatesConnectionConsumesStateAndReturnsRedirect() {
        var now = Instant.now().plusSeconds(600);
        var state = SpotifyOAuthState.create(
                UUID.randomUUID(),
                "state-123",
                now,
                Set.of(SpotifyScope.USER_TOP_READ)
        );
        var properties = spotifyProperties();
        spotifyConnectionService = new SpotifyConnectionService(
                oAuthStateGenerator,
                spotifyConnectionRepository,
                spotifyAuthorizationUrlBuilder,
                spotifyTokenClient,
                tokenEncryptionService,
                spotifyOAuthStateRepository,
                spotifyClient,
                properties
        );

        when(spotifyOAuthStateRepository.findByState("state-123")).thenReturn(Optional.of(state));
        when(spotifyTokenClient.exchangeAuthorizationCode("code-123")).thenReturn(
                new SpotifyTokenResponseDTO(
                        "access-token",
                        "Bearer",
                        "user-top-read user-read-email",
                        3600,
                        "refresh-token"
                )
        );
        when(spotifyClient.getCurrentUserProfile("access-token")).thenReturn(
                new SpotifyUserProfileDTO("spotify-user", "Miles", "AR", "premium")
        );
        when(tokenEncryptionService.encrypt("access-token")).thenReturn("enc-access");
        when(tokenEncryptionService.encrypt("refresh-token")).thenReturn("enc-refresh");
        when(spotifyConnectionRepository.findByUserId(state.getUserId())).thenReturn(Optional.empty());
        when(spotifyConnectionRepository.save(any(SpotifyConnection.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        URI redirect = spotifyConnectionService.handleCallback("code-123", "state-123");

        assertThat(redirect).isEqualTo(URI.create(properties.oauth().redirect().successUri()));
        assertThat(state.getStatus()).isEqualTo(SpotifyOAuthStateStatus.CONSUMED);
        var connectionCaptor = ArgumentCaptor.forClass(SpotifyConnection.class);
        verify(spotifyConnectionRepository).save(connectionCaptor.capture());
        assertThat(connectionCaptor.getValue().getUserId()).isEqualTo(state.getUserId());
        assertThat(connectionCaptor.getValue().getSpotifyUserId()).isEqualTo("spotify-user");
        assertThat(connectionCaptor.getValue().getEncryptedAccessToken()).isEqualTo("enc-access");
        assertThat(connectionCaptor.getValue().getEncryptedRefreshToken()).isEqualTo("enc-refresh");
        assertThat(connectionCaptor.getValue().getGrantedScopes()).isEqualTo("user-read-email user-top-read");
    }

    @Test
    void handleCallbackRejectsExpiredState() {
        var expiredState = SpotifyOAuthState.create(
                UUID.randomUUID(),
                "expired-state",
                Instant.now().minusSeconds(5),
                Set.of(SpotifyScope.USER_TOP_READ)
        );
        spotifyConnectionService = new SpotifyConnectionService(
                oAuthStateGenerator,
                spotifyConnectionRepository,
                spotifyAuthorizationUrlBuilder,
                spotifyTokenClient,
                tokenEncryptionService,
                spotifyOAuthStateRepository,
                spotifyClient,
                spotifyProperties()
        );
        when(spotifyOAuthStateRepository.findByState("expired-state")).thenReturn(Optional.of(expiredState));

        assertThatThrownBy(() -> spotifyConnectionService.handleCallback("code", "expired-state"))
                .isInstanceOf(ExpiredSpotifyOAuthStateException.class);
        assertThat(expiredState.getStatus()).isEqualTo(SpotifyOAuthStateStatus.EXPIRED);
    }

    @Test
    void handleCallbackRejectsSpotifyAccountAlreadyLinkedToAnotherUser() {
        var currentUserId = UUID.randomUUID();
        var otherUserId = UUID.randomUUID();
        var state = SpotifyOAuthState.create(
                currentUserId,
                "state-duplicate",
                Instant.now().plusSeconds(600),
                Set.of(SpotifyScope.USER_TOP_READ)
        );
        spotifyConnectionService = new SpotifyConnectionService(
                oAuthStateGenerator,
                spotifyConnectionRepository,
                spotifyAuthorizationUrlBuilder,
                spotifyTokenClient,
                tokenEncryptionService,
                spotifyOAuthStateRepository,
                spotifyClient,
                spotifyProperties()
        );

        when(spotifyOAuthStateRepository.findByState("state-duplicate")).thenReturn(Optional.of(state));
        when(spotifyTokenClient.exchangeAuthorizationCode("code-123")).thenReturn(
                new SpotifyTokenResponseDTO(
                        "access-token",
                        "Bearer",
                        "user-top-read user-read-email",
                        3600,
                        "refresh-token"
                )
        );
        when(spotifyClient.getCurrentUserProfile("access-token")).thenReturn(
                new SpotifyUserProfileDTO("spotify-user", "Miles", "AR", "premium")
        );
        when(spotifyConnectionRepository.findBySpotifyUserIdAndStatus("spotify-user", SpotifyConnectionStatus.CONNECTED))
                .thenReturn(Optional.of(SpotifyConnection.create(
                        otherUserId,
                        "spotify-user",
                        "Already linked",
                        "AR",
                        "premium",
                        "enc-access",
                        "enc-refresh",
                        "Bearer",
                        "user-top-read",
                        Instant.now().plusSeconds(3600)
                )));

        assertThatThrownBy(() -> spotifyConnectionService.handleCallback("code-123", "state-duplicate"))
                .isInstanceOf(SpotifyAccountAlreadyLinkedException.class)
                .hasMessageContaining("spotify-user");
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
                new SpotifyProperties.Sync("playlist-id", "owner-id", "AR", true, "0 0 * * * *", "UTC", false, 50),
                new SpotifyProperties.Security("secret"),
                new SpotifyProperties.Taste(com.marcoromanofinaa.jazzlogs.spotify.sync.taste.SpotifyTimeRange.MEDIUM_TERM, 10, 10)
        );
    }
}
