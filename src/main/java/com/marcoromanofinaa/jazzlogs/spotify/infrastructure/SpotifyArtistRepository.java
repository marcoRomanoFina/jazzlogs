package com.marcoromanofinaa.jazzlogs.spotify.infrastructure;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyArtist;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyArtistRepository extends JpaRepository<SpotifyArtist, String> {
}
