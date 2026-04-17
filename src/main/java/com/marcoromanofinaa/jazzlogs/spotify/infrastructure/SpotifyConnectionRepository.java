package com.marcoromanofinaa.jazzlogs.spotify.infrastructure;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyConnection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyConnectionRepository extends JpaRepository<SpotifyConnection, UUID> {

    Optional<SpotifyConnection> findTopByOrderByUpdatedAtDesc();
}
