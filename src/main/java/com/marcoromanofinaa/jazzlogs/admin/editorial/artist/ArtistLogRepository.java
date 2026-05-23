package com.marcoromanofinaa.jazzlogs.admin.editorial.artist;

import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.model.ArtistLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ArtistLogRepository extends JpaRepository<ArtistLog, UUID> {

    Optional<ArtistLog> findBySpotifyArtistId(String spotifyArtistId);
}
