package com.marcoromanofinaa.jazzlogs.spotify.connection;

import com.marcoromanofinaa.jazzlogs.spotify.connection.client.SpotifyTokenClient;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotConnectedException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionNotFoundException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyConnectionOwnershipException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenRefreshException;
import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.TokenEncryptionService;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SpotifyTokenService {

    private static final long REFRESH_SKEW_SECONDS = 60L;

    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyTokenClient spotifyTokenClient;
    private final TokenEncryptionService tokenEncryptionService;
    private final Clock clock;

    @Transactional
    public String getValidAccessToken(UUID userId, UUID spotifyConnectionId) {
        var connection = spotifyConnectionRepository.findById(spotifyConnectionId)
                .orElseThrow(() -> new SpotifyConnectionNotFoundException(userId, spotifyConnectionId));

        if (!userId.equals(connection.getUserId())) {
            throw new SpotifyConnectionOwnershipException(userId, spotifyConnectionId);
        }

        if (!connection.isConnected()) {
            throw new SpotifyConnectionNotConnectedException(userId, connection.getStatus());
        }

        var now = Instant.now(clock);
        if (!isExpiredOrAboutToExpire(connection, now)) {
            connection.markUsed();
            spotifyConnectionRepository.save(connection);
            return tokenEncryptionService.decrypt(connection.getEncryptedAccessToken());
        }

        var decryptedRefreshToken = tokenEncryptionService.decrypt(connection.getEncryptedRefreshToken());
        try {
            log.info("Refreshing Spotify access token for connectionId={}", spotifyConnectionId);
            var refreshedToken = spotifyTokenClient.refreshAccessToken(decryptedRefreshToken);
            connection.updateTokens(
                    tokenEncryptionService.encrypt(refreshedToken.accessToken()),
                    refreshedToken.refreshTokenOptional()
                            .map(tokenEncryptionService::encrypt)
                            .orElse(null),
                    refreshedToken.tokenType(),
                    refreshedToken.grantedScopes(),
                    now.plusSeconds(refreshedToken.expiresIn()),
                    now
            );
            connection.markUsed();
            spotifyConnectionRepository.save(connection);
            return refreshedToken.accessToken();
        }
        catch (SpotifyTokenRefreshException exception) {
            if (exception.getRetryAfterSeconds().isPresent()) {
                throw exception;
            }

            connection.markError();
            spotifyConnectionRepository.save(connection);
            throw new SpotifyTokenRefreshException(userId, spotifyConnectionId, exception);
        }
        catch (RuntimeException exception) {
            connection.markError();
            spotifyConnectionRepository.save(connection);
            throw new SpotifyTokenRefreshException(userId, spotifyConnectionId, exception);
        }
    }

    private boolean isExpiredOrAboutToExpire(SpotifyConnection connection, Instant now) {
        return connection.isExpiredAt(now.plusSeconds(REFRESH_SKEW_SECONDS));
    }
}
