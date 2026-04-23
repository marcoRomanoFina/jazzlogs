package com.marcoromanofinaa.jazzlogs.curation.tracknote;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertTrackNoteRequest;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteData;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TrackNoteCurationService {

    private final TrackNoteRepository trackNoteRepository;
    private final SemanticIndexingRequestPublisher indexingRequestPublisher;

    @Transactional
    public boolean upsert(UpsertTrackNoteRequest request) {
        var data = new TrackNoteData(
                request.spotifyTrackId(),
                request.spotifyAlbumId(),
                request.logNumber(),
                request.track(),
                request.album(),
                request.artistId(),
                request.tier(),
                request.isInstrumental(),
                request.isStandout(),
                request.vibe().toArray(String[]::new),
                request.energy(),
                request.moodIntensity(),
                request.accessibility(),
                request.tempoFeel(),
                request.rhythmicFeel(),
                request.trackRole(),
                request.compositionType(),
                request.bestMoment(),
                request.listeningContext().toArray(String[]::new),
                request.whyItHits(),
                request.editorialNote(),
                request.recommendedIf(),
                request.avoidIf(),
                request.instrumentFocus(),
                request.vocalStyle(),
                request.standoutTags().toArray(String[]::new)
        );

        trackNoteRepository.findBySpotifyTrackId(request.spotifyTrackId())
                .ifPresentOrElse(existingTrackNote -> {
                    log.info("Updating track note curation for spotifyTrackId={}", request.spotifyTrackId());
                    existingTrackNote.update(data);
                    trackNoteRepository.save(existingTrackNote);
                }, () -> {
                    log.info("Creating track note curation for spotifyTrackId={}", request.spotifyTrackId());
                    trackNoteRepository.save(TrackNote.create(data));
                });
        indexingRequestPublisher.requestTrackNoteReindex(request.spotifyTrackId());
        return true;
    }
}
