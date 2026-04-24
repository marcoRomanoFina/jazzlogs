package com.marcoromanofinaa.jazzlogs.spotify.auth;

import com.marcoromanofinaa.jazzlogs.spotify.client.SpotifyTokenResponse;
import com.marcoromanofinaa.jazzlogs.spotify.client.SpotifyApiClient;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
// Orquesta el ciclo OAuth de Spotify para la única conexión que mantiene esta
// app: crear la URL de autorización, consumir el callback y refrescar tokens
// cuando el access token guardado expira.
public class SpotifyConnectionService {

    private final SpotifyApiClient spotifyApiClient;
    private final SpotifyConnectionRepository spotifyConnectionRepository;
    private final SpotifyAuthorizationStateService stateService;

    // Inicia el flujo OAuth generando un state propio del backend y delegando
    // la construcción de la URL al cliente de la API de Spotify.
    public String createAuthorizationUrl() {
        var state = stateService.issueState();
        log.info("Creating Spotify authorization URL");
        return spotifyApiClient.buildAuthorizationUrl(state);
    }

    @Transactional
    // Cierra el callback OAuth: verifica el state, intercambia el authorization
    // code por tokens y persiste la conexión resultante.
    public void handleAuthorizationCallback(String code, String state) {
        log.info("Handling Spotify authorization callback");
        if (!stateService.consume(state)) {
            log.warn("Rejected Spotify authorization callback because state was invalid or expired");
            throw new SpotifyException(400, "Invalid or expired Spotify authorization state");
        }

        var tokenResponse = spotifyApiClient.exchangeAuthorizationCode(code);
        saveConnection(tokenResponse);
        log.info("Stored Spotify connection from authorization callback");
    }

    @Transactional
    // Devuelve la conexión guardada más reciente y la refresca en el lugar si
    // el access token expiró, para que los callers siempre reciban un bearer válido.
    public SpotifyConnection getValidConnection() {
        var connection = spotifyConnectionRepository.findTopByOrderByUpdatedAtDesc()
                .orElseThrow(() -> new SpotifyException(400, "Spotify is not connected yet"));

        if (connection.isExpired()) {
            log.info("Refreshing expired Spotify access token");
            var refreshedToken = spotifyApiClient.refreshAccessToken(connection.getRefreshToken());
            connection.updateTokens(
                    refreshedToken.accessToken(),
                    refreshedToken.refreshTokenOptional().orElse(connection.getRefreshToken()),
                    refreshedToken.tokenType(),
                    refreshedToken.scopes(),
                    Instant.now().plusSeconds(refreshedToken.expiresIn())
            );
        }

        log.debug("Returning valid Spotify connection");
        return connection;
    }

    // La app sólo mantiene una conexión de Spotify, así que guardar un set de
    // tokens nuevo reemplaza cualquier fila previa antes de insertar el snapshot actual.
    private void saveConnection(SpotifyTokenResponse tokenResponse) {
        log.info("Replacing persisted Spotify connection with a fresh token snapshot");
        spotifyConnectionRepository.deleteAll();
        spotifyConnectionRepository.save(
                SpotifyConnection.create(
                        tokenResponse.accessToken(),
                        tokenResponse.refreshToken(),
                        tokenResponse.tokenType(),
                        tokenResponse.scopes(),
                        Instant.now().plusSeconds(tokenResponse.expiresIn())
                )
        );
    }
}
