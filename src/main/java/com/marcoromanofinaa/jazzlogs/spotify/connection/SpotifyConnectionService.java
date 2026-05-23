package com.marcoromanofinaa.jazzlogs.spotify.connection;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.integration.SpotifyClient;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenClient;
import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenResponseDTO;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.OAuthStateGenerator;
import com.marcoromanofinaa.jazzlogs.spotify.exception.ConsumedSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.ExpiredSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.InvalidSpotifyOAuthStateException;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyAuthorizationUrlBuilder;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthState;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthStateRepository;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyOAuthStateStatus;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.TokenEncryptionService;
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyConnectionService {

    private final OAuthStateGenerator oAuthStateGenerator;
    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyAuthorizationUrlBuilder spotifyAuthorizationUrlBuilder;
    private final SpotifyTokenClient spotifyTokenClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final SpotifyOAuthStateRepository spotifyOAuthStateRepository;
    private final SpotifyClient spotifyClient;
    private final SpotifyProperties spotifyProperties;

    public SpotifyAuthorizationUrlDTO createAuthorizationUrl(
            UUID userId,
            Set<SpotifyScope> requestedScopes
    ) {
        if (requestedScopes == null || requestedScopes.isEmpty()) {
            throw new IllegalArgumentException("At least one Spotify scope is required");
        }

        var stateValue = oAuthStateGenerator.generate();
        spotifyOAuthStateRepository.save(
                SpotifyOAuthState.create(
                        userId,
                        stateValue,
                        Instant.now().plus(spotifyProperties.oauth().stateTtl()),
                        requestedScopes
                )
        );

        log.info("Creating Spotify authorization URL for userId={} with {} scopes", userId, requestedScopes.size());
        return new SpotifyAuthorizationUrlDTO(spotifyAuthorizationUrlBuilder.build(stateValue, requestedScopes));
    }

    @Transactional
    public URI handleCallback(String code, String state) {
        log.info("Handling Spotify authorization callback");
        var stateRecord = spotifyOAuthStateRepository.findByState(state)
                .orElseThrow(() -> new InvalidSpotifyOAuthStateException(state));

        var now = Instant.now();
        if (stateRecord.getStatus() == SpotifyOAuthStateStatus.CONSUMED) {
            throw new ConsumedSpotifyOAuthStateException(state);
        }
        if (stateRecord.getStatus() == SpotifyOAuthStateStatus.EXPIRED) {
            throw new ExpiredSpotifyOAuthStateException(state);
        }
        if (!stateRecord.getExpiresAt().isAfter(now)) {
            stateRecord.markExpired();
            log.warn("Rejected Spotify authorization callback because state expired");
            throw new ExpiredSpotifyOAuthStateException(state);
        }

        var tokenResponse = spotifyTokenClient.exchangeAuthorizationCode(code);
        saveConnection(stateRecord, tokenResponse, now);
        stateRecord.markConsumed(now);

        log.info("Stored Spotify connection from authorization callback for userId={}", stateRecord.getUserId());
        return URI.create(spotifyProperties.oauth().redirect().successUri());
    }

    private void saveConnection(
            SpotifyOAuthState stateRecord,
            SpotifyTokenResponseDTO tokenResponse,
            Instant now
    ) {
        var spotifyUserProfile = spotifyClient.getCurrentUserProfile(tokenResponse.accessToken());
        var encryptedAccessToken = tokenEncryptionService.encrypt(tokenResponse.accessToken());

        var existingConnection = spotifyConnectionRepository.findByUserId(stateRecord.getUserId());

        if (existingConnection.isPresent()) {
            var connection = existingConnection.get();
            var refreshToken = tokenResponse.refreshTokenOptional()
                    .map(tokenEncryptionService::encrypt)
                    .orElse(connection.getEncryptedRefreshToken());

            connection.updateFromOAuthCallback(
                    spotifyUserProfile.spotifyUserId(),
                    spotifyUserProfile.displayName(),
                    spotifyUserProfile.country(),
                    spotifyUserProfile.product(),
                    encryptedAccessToken,
                    refreshToken,
                    tokenResponse.tokenType(),
                    tokenResponse.grantedScopes(),
                    now.plusSeconds(tokenResponse.expiresIn())
            );
            return;
        }

        spotifyConnectionRepository.save(
                SpotifyConnection.create(
                        stateRecord.getUserId(),
                        spotifyUserProfile.spotifyUserId(),
                        spotifyUserProfile.displayName(),
                        spotifyUserProfile.country(),
                        spotifyUserProfile.product(),
                        encryptedAccessToken,
                        tokenEncryptionService.encrypt(tokenResponse.refreshToken()),
                        tokenResponse.tokenType(),
                        tokenResponse.grantedScopes(),
                        now.plusSeconds(tokenResponse.expiresIn())
                )
        );
    }

}
