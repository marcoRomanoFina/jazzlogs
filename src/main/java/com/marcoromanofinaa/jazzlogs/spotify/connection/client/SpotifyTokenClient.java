package com.marcoromanofinaa.jazzlogs.spotify.connection.client;

import com.marcoromanofinaa.jazzlogs.spotify.config.SpotifyProperties;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyRateLimitException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenExchangeException;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyTokenRefreshException;
import java.util.Optional;
import java.util.function.BiFunction;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
@RequiredArgsConstructor
public class SpotifyTokenClient {

    private final @Qualifier("spotifyTokenRestClient") RestClient spotifyTokenRestClient;
    private final SpotifyProperties spotifyProperties;

    public SpotifyTokenResponseDTO exchangeAuthorizationCode(String code) {
        ensureConfiguredExchangeProperties();

        return postForm(
                spotifyProperties.oauth().tokenUrl(),
                formData(
                        "grant_type", "authorization_code",
                        "code", code,
                        "redirect_uri", spotifyProperties.oauth().redirectUri(),
                        "client_id", spotifyProperties.oauth().clientId(),
                        "client_secret", spotifyProperties.oauth().clientSecret()
                ),
                SpotifyTokenResponseDTO.class,
                "Spotify authorization code response was empty",
                this::mapTokenExchangeException
        );
    }

    public SpotifyTokenResponseDTO refreshAccessToken(String refreshToken) {
        ensureConfiguredRefreshProperties();

        return postForm(
                spotifyProperties.oauth().tokenUrl(),
                formData(
                        "grant_type", "refresh_token",
                        "refresh_token", refreshToken,
                        "client_id", spotifyProperties.oauth().clientId(),
                        "client_secret", spotifyProperties.oauth().clientSecret()
                ),
                SpotifyTokenResponseDTO.class,
                "Spotify refresh token response was empty",
                this::mapTokenRefreshException
        );
    }

    private <T> T postForm(
            String uri,
            LinkedMultiValueMap<String, String> form,
            Class<T> responseType,
            String emptyBodyMessage,
            BiFunction<String, RestClientResponseException, RuntimeException> exceptionFactory
    ) {
        try {
            var response = spotifyTokenRestClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(responseType);

            if (response == null) {
                throw new SpotifyTokenExchangeException(emptyBodyMessage);
            }

            return response;
        }
        catch (RestClientResponseException exception) {
            throw exceptionFactory.apply("Spotify token request failed", exception);
        }
    }

    private LinkedMultiValueMap<String, String> formData(String... entries) {
        var form = new LinkedMultiValueMap<String, String>();
        for (int index = 0; index < entries.length; index += 2) {
            form.add(entries[index], entries[index + 1]);
        }
        return form;
    }

    private void ensureConfiguredExchangeProperties() {
        requireText(spotifyProperties.oauth().clientId(), "Spotify client ID is not configured");
        requireText(spotifyProperties.oauth().clientSecret(), "Spotify client secret is not configured");
        requireText(spotifyProperties.oauth().redirectUri(), "Spotify redirect URI is not configured");
        requireText(spotifyProperties.oauth().tokenUrl(), "Spotify token URL is not configured");
    }

    private void ensureConfiguredRefreshProperties() {
        requireText(spotifyProperties.oauth().clientId(), "Spotify client ID is not configured");
        requireText(spotifyProperties.oauth().clientSecret(), "Spotify client secret is not configured");
        requireText(spotifyProperties.oauth().tokenUrl(), "Spotify token URL is not configured");
    }

    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException(message);
        }
    }

    private RuntimeException mapTokenExchangeException(String message, RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 429) {
            var retryAfterSeconds = parseRetryAfterSeconds(exception);
            return new SpotifyRateLimitException(
                    retryAfterSeconds
                            .map(seconds -> "Spotify token exchange rate limited. Retry after %d seconds".formatted(seconds))
                            .orElse("Spotify token exchange rate limited. Retry later."),
                    retryAfterSeconds
            );
        }

        return new SpotifyTokenExchangeException(
                "%s with status %d".formatted(message, exception.getStatusCode().value()),
                exception
        );
    }

    private RuntimeException mapTokenRefreshException(String message, RestClientResponseException exception) {
        if (exception.getStatusCode().value() == 429) {
            var retryAfterSeconds = parseRetryAfterSeconds(exception);
            return new SpotifyTokenRefreshException(
                    retryAfterSeconds
                            .map(seconds -> "Spotify token refresh rate limited. Retry after %d seconds".formatted(seconds))
                            .orElse("Spotify token refresh rate limited. Retry later."),
                    retryAfterSeconds,
                    exception
            );
        }

        return new SpotifyTokenRefreshException(
                "%s with status %d".formatted(message, exception.getStatusCode().value()),
                exception
        );
    }

    private Optional<Integer> parseRetryAfterSeconds(RestClientResponseException exception) {
        if (exception.getResponseHeaders() == null) {
            return Optional.empty();
        }

        var retryAfter = exception.getResponseHeaders().getFirst("Retry-After");
        if (retryAfter == null || retryAfter.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Integer.parseInt(retryAfter));
        }
        catch (NumberFormatException ignored) {
            return Optional.empty();
        }
    }
}
