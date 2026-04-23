package com.marcoromanofinaa.jazzlogs.logbook.artistprofile;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, UUID> {

    List<ArtistProfile> findAllBySpotifyArtistIdIn(Collection<String> spotifyArtistIds);

    Optional<ArtistProfile> findBySpotifyArtistId(String spotifyArtistId);
}
