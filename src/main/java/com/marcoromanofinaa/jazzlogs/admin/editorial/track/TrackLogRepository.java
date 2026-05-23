package com.marcoromanofinaa.jazzlogs.admin.editorial.track;

import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackLogRepository extends JpaRepository<TrackLog, UUID> {

    Optional<TrackLog> findBySpotifyTrackId(String spotifyTrackId);
}
