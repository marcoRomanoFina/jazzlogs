package com.marcoromanofinaa.jazzlogs.spotify.application;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyConnection;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyApiClient;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyConnectionRepository;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyTokenResponse;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
// Orchestrates the Spotify OAuth lifecycle for the single connection this app
// keeps: create the authorization URL, consume the callback, and refresh tokens
// whenever the stored access token expires.
public class SpotifyConnectionService {

    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyAuthorizationStateService stateService;

    // Starts the OAuth flow by issuing a backend-owned state value and
    // delegating URL construction to the Spotify API client.
    public String createAuthorizationUrl() {
        var state = stateService.issueState();
        return spotifyApiClient.buildAuthorizationUrl(state);
    }

    @Transactional
    // Finalizes the OAuth callback: verifies the state, exchanges the
    // authorization code for tokens, and persists the resulting connection.
    public void handleAuthorizationCallback(String code, String state) {
        if (!stateService.consume(state)) {
            throw new SpotifyException(400, "Invalid or expired Spotify authorization state");
        }

        var tokenResponse = spotifyApiClient.exchangeAuthorizationCode(code);
        saveConnection(tokenResponse);
    }

    @Transactional
    // Returns the latest stored connection, refreshing it in place when the
    // access token has expired so callers always get a usable bearer token.
    public SpotifyConnection getValidConnection() {
        var connection = spotifyConnectionRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new SpotifyException(400, "Spotify is not connected yet"));

        if (connection.isExpired()) {
            var refreshedToken = spotifyApiClient.refreshAccessToken(connection.getRefreshToken());
            connection.updateTokens(
                    refreshedToken.accessToken(),
                    refreshedToken.refreshTokenOptional().orElse(connection.getRefreshToken()),
                    refreshedToken.tokenType(),
                    refreshedToken.scopes(),
                    OffsetDateTime.now().plusSeconds(refreshedToken.expiresIn())
            );
        }

        return connection;
    }

    // The app only keeps one Spotify connection, so saving a fresh token set
    // replaces any previous row before inserting the new snapshot.
    private void saveConnection(SpotifyTokenResponse tokenResponse) {
        spotifyConnectionRepository.deleteAll();
        spotifyConnectionRepository.save(
                SpotifyConnection.create(
                        tokenResponse.accessToken(),
                        tokenResponse.refreshToken(),
                        tokenResponse.tokenType(),
                        tokenResponse.scopes(),
                        OffsetDateTime.now().plusSeconds(tokenResponse.expiresIn())
                )
        );
    }
}
