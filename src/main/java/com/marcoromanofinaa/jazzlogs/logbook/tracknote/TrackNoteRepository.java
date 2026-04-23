package com.marcoromanofinaa.jazzlogs.logbook.tracknote;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackNoteRepository extends JpaRepository<TrackNote, UUID> {

    List<TrackNote> findAllBySpotifyTrackIdIn(Collection<String> spotifyTrackIds);

    Optional<TrackNote> findBySpotifyTrackId(String spotifyTrackId);
}
