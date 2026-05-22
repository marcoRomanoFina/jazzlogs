package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "spotify_oauth_states")
public class SpotifyOAuthState {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(nullable = false, unique = true, length = 255)
    private String state;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SpotifyOAuthStateStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "requested_scopes", nullable = false, columnDefinition = "text[]")
    private String[] requestedScopes;

    private SpotifyOAuthState(UUID userId, String state, Instant expiresAt, Set<SpotifyScope> requestedScopes) {
        this.userId = userId;
        this.state = state;
        this.status = SpotifyOAuthStateStatus.PENDING;
        this.expiresAt = expiresAt;
        this.requestedScopes = requestedScopes.stream()
                .map(SpotifyScope::value)
                .sorted()
                .toArray(String[]::new);
    }

    public static SpotifyOAuthState create(
            UUID userId,
            String state,
            Instant expiresAt,
            Set<SpotifyScope> requestedScopes
    ) {
        return new SpotifyOAuthState(userId, state, expiresAt, requestedScopes);
    }

    public boolean isPendingAndNotExpired(Instant now) {
        return status == SpotifyOAuthStateStatus.PENDING && expiresAt.isAfter(now);
    }

    public void markConsumed(Instant consumedAt) {
        this.status = SpotifyOAuthStateStatus.CONSUMED;
        this.consumedAt = consumedAt;
    }

    public void markExpired() {
        this.status = SpotifyOAuthStateStatus.EXPIRED;
    }
}
