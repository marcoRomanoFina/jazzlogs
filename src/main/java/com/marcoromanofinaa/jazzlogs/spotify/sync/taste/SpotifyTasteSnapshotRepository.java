package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyTasteSnapshotRepository extends JpaRepository<SpotifyTasteSnapshot, UUID> {

    Optional<SpotifyTasteSnapshot> findTopByUserIdOrderByGeneratedAtDesc(UUID userId);

    Optional<SpotifyTasteSnapshot> findTopBySpotifyConnectionIdOrderByGeneratedAtDesc(UUID spotifyConnectionId);
}
