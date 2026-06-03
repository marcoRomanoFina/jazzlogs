package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumLogRepository extends JpaRepository<AlbumLog, UUID> {

    Optional<AlbumLog> findBySpotifyAlbumId(String spotifyAlbumId);

    Optional<AlbumLog> findByLogNumber(Integer logNumber);

    Optional<AlbumLog> findFirstByAlbumNameIgnoreCase(String albumName);

    @Query("select a from AlbumLog a where lower(a.albumName) in :albumNames")
    List<AlbumLog> findByAlbumNameInIgnoreCase(@Param("albumNames") Collection<String> albumNames);

    List<AlbumLog> findBySpotifyAlbumIdIn(Collection<String> spotifyAlbumIds);
}
