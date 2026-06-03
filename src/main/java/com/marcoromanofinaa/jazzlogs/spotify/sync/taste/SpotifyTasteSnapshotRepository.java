package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyTasteSnapshotRepository extends JpaRepository<SpotifyTasteSnapshot, UUID> {

    Optional<SpotifyTasteSnapshot> findTopByUserIdOrderByGeneratedAtDesc(UUID userId);

    Optional<SpotifyTasteSnapshot> findTopBySpotifyConnectionIdOrderByGeneratedAtDesc(UUID spotifyConnectionId);

    @Modifying
    @Query(value = "delete from spotify_taste_snapshots where user_id = :userId", nativeQuery = true)
    void deleteAllByUserId(@Param("userId") UUID userId);
}
