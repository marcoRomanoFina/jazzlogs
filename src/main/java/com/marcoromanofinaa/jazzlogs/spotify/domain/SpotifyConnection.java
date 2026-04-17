package com.marcoromanofinaa.jazzlogs.spotify.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "spotify_connections")
public class SpotifyConnection {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "access_token", nullable = false, columnDefinition = "text")
    private String accessToken;

    @Column(name = "refresh_token", nullable = false, columnDefinition = "text")
    private String refreshToken;

    @Column(name = "token_type", nullable = false, length = 32)
    private String tokenType;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "scopes", nullable = false, columnDefinition = "text[]")
    private String[] scopes;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Builder
    private SpotifyConnection(
            String accessToken,
            String refreshToken,
            String tokenType,
            String[] scopes,
            OffsetDateTime expiresAt
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
    }

    public static SpotifyConnection create(
            String accessToken,
            String refreshToken,
            String tokenType,
            String[] scopes,
            OffsetDateTime expiresAt
    ) {
        return SpotifyConnection.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType(tokenType)
                .scopes(scopes)
                .expiresAt(expiresAt)
                .build();
    }

    public void updateTokens(
            String accessToken,
            String refreshToken,
            String tokenType,
            String[] scopes,
            OffsetDateTime expiresAt
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.scopes = scopes;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt == null || expiresAt.minusMinutes(1).isBefore(OffsetDateTime.now());
    }
}
