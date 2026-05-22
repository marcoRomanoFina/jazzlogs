package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyTrackRepository extends JpaRepository<SpotifyTrack, UUID> {

    Optional<SpotifyTrack> findBySpotifyTrackId(String spotifyTrackId);

}
