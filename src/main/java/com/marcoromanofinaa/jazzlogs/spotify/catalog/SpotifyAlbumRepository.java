package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpotifyAlbumRepository extends JpaRepository<SpotifyAlbum, UUID> {

    Optional<SpotifyAlbum> findBySpotifyAlbumId(String spotifyAlbumId);

    List<SpotifyAlbum> findAllBySpotifyAlbumIdIn(Collection<String> spotifyAlbumIds);
}
