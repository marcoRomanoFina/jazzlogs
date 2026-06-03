package com.marcoromanofinaa.jazzlogs.spotify.connection;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyConnectionRepository extends JpaRepository<SpotifyConnection, UUID> {

    Optional<SpotifyConnection> findByUserId(UUID userId);

    Optional<SpotifyConnection> findByUserIdAndStatus(
            UUID userId,
            SpotifyConnectionStatus status
    );

    Optional<SpotifyConnection> findBySpotifyUserIdAndStatus(
            String spotifyUserId,
            SpotifyConnectionStatus status
    );

    Optional<SpotifyConnection> findFirstByStatusOrderByConnectedAtAsc(
            SpotifyConnectionStatus status
    );

    default Optional<SpotifyConnection> findConnectedByUserId(UUID userId) {
        return findByUserIdAndStatus(userId, SpotifyConnectionStatus.CONNECTED);
    }
}
