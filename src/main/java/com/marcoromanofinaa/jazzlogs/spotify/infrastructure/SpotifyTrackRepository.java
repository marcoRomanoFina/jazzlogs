package com.marcoromanofinaa.jazzlogs.spotify.infrastructure;

import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyTrack;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotifyTrackRepository extends JpaRepository<SpotifyTrack, String> {

    long deleteBySourcePlaylistId(String sourcePlaylistId);

    List<SpotifyTrack> findAllBySourcePlaylistIdOrderByNameAsc(String sourcePlaylistId);

    @EntityGraph(attributePaths = {"album", "mainArtist", "secondaryArtists"})
    List<SpotifyTrack> findAllBySourcePlaylistId(String sourcePlaylistId);
}
