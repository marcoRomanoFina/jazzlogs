package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AlbumLogRepository extends JpaRepository<AlbumLog, UUID> {

    Optional<AlbumLog> findBySpotifyAlbumId(String spotifyAlbumId);

    Optional<AlbumLog> findByLogNumber(Integer logNumber);
}
