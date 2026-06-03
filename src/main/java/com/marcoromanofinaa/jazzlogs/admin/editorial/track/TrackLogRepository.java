package com.marcoromanofinaa.jazzlogs.admin.editorial.track;

import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TrackLogRepository extends JpaRepository<TrackLog, UUID> {

    Optional<TrackLog> findBySpotifyTrackId(String spotifyTrackId);

    Optional<TrackLog> findFirstByTrackNameIgnoreCase(String trackName);

    @Query("select t from TrackLog t where lower(t.trackName) in :trackNames")
    List<TrackLog> findByTrackNameInIgnoreCase(@Param("trackNames") Collection<String> trackNames);
}
