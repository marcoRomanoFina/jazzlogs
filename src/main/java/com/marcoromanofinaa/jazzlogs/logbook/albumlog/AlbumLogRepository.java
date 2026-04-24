package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlbumLogRepository extends JpaRepository<AlbumLog, UUID> {

    long deleteByLogNumber(Integer logNumber);

    List<AlbumLog> findAllByOrderByLogNumberAsc();

    List<AlbumLog> findAllBySpotifyAlbumIsNullOrderByLogNumberAsc();

    List<AlbumLog> findAllByLogNumberIn(Collection<Integer> logNumbers);

    Optional<AlbumLog> findByLogNumber(Integer logNumber);
}
