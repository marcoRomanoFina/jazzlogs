package com.marcoromanofinaa.jazzlogs.spotify.connection.oauth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SpotifyOAuthStateRepository extends JpaRepository<SpotifyOAuthState, UUID> {

    Optional<SpotifyOAuthState> findByState(String state);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update SpotifyOAuthState state
            set state.status = :expiredStatus
            where state.status = :pendingStatus
              and state.expiresAt < :now
            """)
    int expirePendingStates(
            @Param("pendingStatus") SpotifyOAuthStateStatus pendingStatus,
            @Param("expiredStatus") SpotifyOAuthStateStatus expiredStatus,
            @Param("now") Instant now
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            delete from SpotifyOAuthState state
            where state.status in :statuses
              and state.createdAt < :createdAt
            """)
    int deleteAllByStatusInAndCreatedAtBefore(
            @Param("statuses") List<SpotifyOAuthStateStatus> statuses,
            @Param("createdAt") Instant createdAt
    );
}
