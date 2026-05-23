package com.marcoromanofinaa.jazzlogs.spotify.connection.client;

import java.util.Optional;

public record SpotifyTokenResponseDTO(
        String access_token,
        String token_type,
        String scope,
        Integer expires_in,
        String refresh_token
) {

    public String accessToken() {
        return access_token;
    }

    public String tokenType() {
        return token_type;
    }

    public String grantedScopes() {
        return scope == null ? "" : scope.trim();
    }

    public String[] scopes() {
        return scope == null || scope.isBlank() ? new String[0] : scope.split(" ");
    }

    public int expiresIn() {
        return expires_in == null ? 0 : expires_in;
    }

    public String refreshToken() {
        return refresh_token;
    }

    public Optional<String> refreshTokenOptional() {
        return Optional.ofNullable(refresh_token)
                .filter(value -> !value.isBlank());
    }
}
