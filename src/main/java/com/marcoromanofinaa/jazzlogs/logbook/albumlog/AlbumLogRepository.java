package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumLogRepository extends JpaRepository<AlbumLog, UUID> {

    long deleteByLogNumber(Integer logNumber);

    List<AlbumLog> findAllByOrderByLogNumberAsc();

    List<AlbumLog> findAllByLogNumberIn(Collection<Integer> logNumbers);

    Optional<AlbumLog> findByLogNumber(Integer logNumber);

    @EntityGraph(attributePaths = "spotifyAlbum")
    Optional<AlbumLog> findWithSpotifyAlbumById(UUID id);

    @EntityGraph(attributePaths = "spotifyAlbum")
    Optional<AlbumLog> findWithSpotifyAlbumByLogNumber(Integer logNumber);
}
