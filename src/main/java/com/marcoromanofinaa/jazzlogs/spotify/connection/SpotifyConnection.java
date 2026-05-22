package com.marcoromanofinaa.jazzlogs.spotify.connection;

import com.marcoromanofinaa.jazzlogs.spotify.connection.oauth.SpotifyScope;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "spotify_connections",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_spotify_connections_user_id",
                        columnNames = "user_id"
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SpotifyConnection {

    @Id
    @GeneratedValue(strategy = jakarta.persistence.GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false, unique = true)
    private UUID userId;

    @Column(name = "spotify_user_id", nullable = false)
    private String spotifyUserId;

    @Column(name = "spotify_display_name")
    private String spotifyDisplayName;

    @Column(name = "spotify_country", length = 16)
    private String spotifyCountry;

    @Column(name = "spotify_product", length = 64)
    private String spotifyProduct;

    @Column(name = "encrypted_access_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "encrypted_refresh_token", nullable = false, columnDefinition = "TEXT")
    private String encryptedRefreshToken;

    @Column(name = "token_type", nullable = false)
    private String tokenType;

    @Column(name = "granted_scopes", nullable = false, columnDefinition = "TEXT")
    private String grantedScopes;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SpotifyConnectionStatus status;

    @Column(name = "connected_at", nullable = false)
    private Instant connectedAt;

    @Column(name = "last_refreshed_at")
    private Instant lastRefreshedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "disconnected_at")
    private Instant disconnectedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    private SpotifyConnection(
            UUID userId,
            String spotifyUserId,
            String spotifyDisplayName,
            String spotifyCountry,
            String spotifyProduct,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenType,
            String grantedScopes,
            Instant expiresAt,
            Instant connectedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.userId = userId;
        this.spotifyUserId = spotifyUserId;
        this.spotifyDisplayName = spotifyDisplayName;
        this.spotifyCountry = spotifyCountry;
        this.spotifyProduct = spotifyProduct;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.tokenType = tokenType;
        this.grantedScopes = grantedScopes;
        this.expiresAt = expiresAt;
        this.status = SpotifyConnectionStatus.CONNECTED;
        this.connectedAt = connectedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static SpotifyConnection create(
            UUID userId,
            String spotifyUserId,
            String spotifyDisplayName,
            String spotifyCountry,
            String spotifyProduct,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenType,
            String grantedScopes,
            Instant expiresAt
    ) {
        var now = Instant.now();
        return new SpotifyConnection(
                userId,
                spotifyUserId,
                spotifyDisplayName,
                spotifyCountry,
                spotifyProduct,
                encryptedAccessToken,
                encryptedRefreshToken,
                tokenType,
                normalizeScopes(grantedScopes),
                expiresAt,
                now,
                now,
                now
        );
    }

    public void updateFromOAuthCallback(
            String spotifyUserId,
            String spotifyDisplayName,
            String spotifyCountry,
            String spotifyProduct,
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenType,
            String grantedScopes,
            Instant expiresAt
    ) {
        var now = Instant.now();
        this.spotifyUserId = spotifyUserId;
        this.spotifyDisplayName = spotifyDisplayName;
        this.spotifyCountry = spotifyCountry;
        this.spotifyProduct = spotifyProduct;
        this.encryptedAccessToken = encryptedAccessToken;
        this.encryptedRefreshToken = encryptedRefreshToken;
        this.tokenType = tokenType;
        this.grantedScopes = normalizeScopes(grantedScopes);
        this.expiresAt = expiresAt;
        this.status = SpotifyConnectionStatus.CONNECTED;
        this.connectedAt = now;
        this.disconnectedAt = null;
        this.updatedAt = now;
    }

    public void updateTokens(
            String encryptedAccessToken,
            String encryptedRefreshToken,
            String tokenType,
            String grantedScopes,
            Instant expiresAt,
            Instant refreshedAt
    ) {
        this.encryptedAccessToken = encryptedAccessToken;
        if (encryptedRefreshToken != null && !encryptedRefreshToken.isBlank()) {
            this.encryptedRefreshToken = encryptedRefreshToken;
        }
        this.tokenType = tokenType;
        this.grantedScopes = normalizeScopes(grantedScopes);
        this.expiresAt = expiresAt;
        this.lastRefreshedAt = refreshedAt;
        this.updatedAt = refreshedAt;
        this.status = SpotifyConnectionStatus.CONNECTED;
    }

    public void markUsed() {
        var now = Instant.now();
        this.lastUsedAt = now;
        this.updatedAt = now;
    }

    public void disconnect() {
        var now = Instant.now();
        this.status = SpotifyConnectionStatus.DISCONNECTED;
        this.disconnectedAt = now;
        this.updatedAt = now;
    }

    public void markError() {
        this.status = SpotifyConnectionStatus.ERROR;
        this.updatedAt = Instant.now();
    }

    public boolean isConnected() {
        return status == SpotifyConnectionStatus.CONNECTED;
    }

    public boolean isExpiredAt(Instant now) {
        return expiresAt == null || !expiresAt.isAfter(now);
    }

    public boolean hasScope(SpotifyScope scope) {
        return parseGrantedScopes().contains(scope.value());
    }

    public boolean hasScopes(Set<SpotifyScope> requiredScopes) {
        var grantedScopeValues = parseGrantedScopes();
        return requiredScopes.stream()
                .map(SpotifyScope::value)
                .allMatch(grantedScopeValues::contains);
    }

    private Set<String> parseGrantedScopes() {
        if (grantedScopes == null || grantedScopes.isBlank()) {
            return Set.of();
        }

        return Arrays.stream(grantedScopes.split(" "))
                .filter(scope -> !scope.isBlank())
                .collect(Collectors.toSet());
    }

    private static String normalizeScopes(String grantedScopes) {
        if (grantedScopes == null || grantedScopes.isBlank()) {
            return "";
        }

        return Arrays.stream(grantedScopes.split(" "))
                .filter(scope -> !scope.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.joining(" "));
    }
}
