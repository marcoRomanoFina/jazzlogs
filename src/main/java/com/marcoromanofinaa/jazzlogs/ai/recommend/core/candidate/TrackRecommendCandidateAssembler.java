package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TrackRecommendCandidateAssembler implements RecommendCandidateAssembler<TrackRecommendCandidate> {

    private static final String SEMANTIC_DOCUMENT_ID_METADATA = "semanticDocumentId";
    private static final String SPOTIFY_TRACK_ID_METADATA = "spotifyTrackId";

    private final AlbumLogRepository albumLogRepository;
    private final TrackNoteRepository trackNoteRepository;
    private final SpotifyTrackRepository spotifyTrackRepository;

    @Override
    public TrackRecommendCandidate assemble(Document document) {
        var spotifyTrackId = requiredMetadata(document, SPOTIFY_TRACK_ID_METADATA);
        var trackNote = trackNoteRepository.findBySpotifyTrackId(spotifyTrackId)
                .orElseThrow(() -> new IllegalArgumentException("TrackNote not found for spotifyTrackId=" + spotifyTrackId));
        var spotifyTrack = spotifyTrackRepository.findWithAlbumAndMainArtistBySpotifyTrackId(spotifyTrackId);
        var albumLog = albumLogRepository.findWithSpotifyAlbumByLogNumber(trackNote.getLogNumber()).orElse(null);
        var jazzLogsCaptionEssence = albumLog != null ? albumLog.getCaption() : null;
        var mainArtists = Optional.ofNullable(albumLog)
                .map(log -> log.getMainArtists().stream()
                        .map(mainArtist -> mainArtist.artistName())
                        .toList())
                .orElse(List.of());
        var albumPersonnel = Optional.ofNullable(albumLog)
                .map(log -> log.getPersonnel().stream()
                        .map(person -> "%s - %s".formatted(person.name(), person.role()))
                        .toList())
                .orElse(List.of());
        var artistContext = !albumPersonnel.isEmpty()
                ? albumPersonnel
                : mainArtists;

        return new TrackRecommendCandidate(
                document.getScore(),
                requiredMetadata(document, SEMANTIC_DOCUMENT_ID_METADATA),
                trackNote.getId(),
                trackNote.getSpotifyTrackId(),
                trackNote.getTrack(),
                trackNote.getAlbum(),
                spotifyTrack
                        .map(track -> track.getMainArtist())
                        .map(artist -> artist.getName())
                        .or(() -> Optional.ofNullable(albumLog).map(log -> log.getArtist()))
                        .orElse(null),
                trackNote.getLogNumber(),
                new TrackRecommendDecisionContext(
                        trackNote.getTier(),
                        trackNote.isInstrumental(),
                        trackNote.isStandout(),
                        jazzLogsCaptionEssence,
                        mainArtists,
                        artistContext,
                        toList(trackNote.getVibe()),
                        trackNote.getEnergy(),
                        trackNote.getMoodIntensity(),
                        trackNote.getAccessibility(),
                        trackNote.getTempoFeel(),
                        trackNote.getRhythmicFeel(),
                        trackNote.getTrackRole(),
                        trackNote.getCompositionType(),
                        trackNote.getBestMoment(),
                        toList(trackNote.getListeningContext()),
                        trackNote.getWhyItHits(),
                        trackNote.getEditorialNote(),
                        trackNote.getRecommendedIf(),
                        trackNote.getAvoidIf(),
                        trackNote.getInstrumentFocus(),
                        trackNote.getVocalStyle(),
                        toList(trackNote.getStandoutTags()),
                        albumPersonnel
                ),
                new TrackRecommendDeliveryMetadata(
                        spotifyTrack
                                .map(track -> track.getSpotifyUrl())
                                .orElseGet(() -> "https://open.spotify.com/track/" + trackNote.getSpotifyTrackId()),
                        spotifyTrack
                                .map(track -> track.getAlbum())
                                .map(album -> album.getCoverImageUrl())
                                .or(() -> Optional.ofNullable(albumLog)
                                        .map(log -> log.getSpotifyAlbum())
                                        .map(album -> album.getCoverImageUrl()))
                                .orElse(null)
                )
        );
    }

    private String requiredMetadata(Document document, String key) {
        return Optional.ofNullable(document.getMetadata().get(key))
                .map(Object::toString)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing metadata key '%s' in retrieved document".formatted(key)
                ));
    }

    private List<String> toList(String[] values) {
        return Optional.ofNullable(values)
                .map(Arrays::asList)
                .orElseGet(List::of);
    }
}
