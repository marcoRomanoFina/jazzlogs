package com.marcoromanofinaa.jazzlogs.spotify.infrastructure;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyAlbumRepository extends JpaRepository<SpotifyAlbum, String> {

    long deleteBySourcePlaylistId(String sourcePlaylistId);

    List<SpotifyAlbum> findAllBySourcePlaylistIdOrderByNameAsc(String sourcePlaylistId);
}
