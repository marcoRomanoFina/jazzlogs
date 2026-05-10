package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteData;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;

@ExtendWith(MockitoExtension.class)
class TrackRecommendCandidateAssemblerTest {

    @Mock
    private TrackNoteRepository trackNoteRepository;

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Mock
    private SpotifyTrackRepository spotifyTrackRepository;

    @Test
    void assemblesTrackCandidateFromDocumentTrackNoteAndSpotifyTrack() throws Exception {
        var trackNoteId = UUID.randomUUID();
        var trackNote = buildTrackNote(trackNoteId);
        var spotifyTrack = SpotifyTrack.builder()
                .spotifyTrackId("spotify-track-1")
                .name("Spunky")
                .spotifyUrl("https://open.spotify.com/track/spotify-track-1")
                .album(SpotifyAlbum.builder()
                        .spotifyAlbumId("spotify-album-1")
                        .name("Spunky")
                        .coverImageUrl("https://i.scdn.co/image/spunky")
                        .build())
                .mainArtist(SpotifyArtist.builder()
                        .spotifyArtistId("artist-1")
                        .name("Monty Alexander")
                        .build())
                .secondaryArtists(Set.of())
                .durationMs(321000)
                .trackNumber(1)
                .addedToPlaylistAt(Instant.parse("2026-04-01T00:00:00Z"))
                .build();
        var document = Document.builder()
                .id("doc-2")
                .text("track semantic text")
                .metadata(Map.of(
                        "sourceId", trackNoteId.toString(),
                        "semanticDocumentId", "TRACK_NOTE:" + trackNoteId,
                        "spotifyTrackId", "spotify-track-1"
                ))
                .score(0.88)
                .build();

        when(trackNoteRepository.findBySpotifyTrackId("spotify-track-1")).thenReturn(Optional.of(trackNote));
        when(albumLogRepository.findWithSpotifyAlbumByLogNumber(1)).thenReturn(Optional.of(buildAlbumLogForTrackContext()));
        when(spotifyTrackRepository.findWithAlbumAndMainArtistBySpotifyTrackId("spotify-track-1"))
                .thenReturn(Optional.of(spotifyTrack));

        var candidate = new TrackRecommendCandidateAssembler(albumLogRepository, trackNoteRepository, spotifyTrackRepository)
                .assemble(document);

        assertThat(candidate.similarityScore()).isEqualTo(0.88);
        assertThat(candidate.semanticDocumentId()).isEqualTo("TRACK_NOTE:" + trackNoteId);
        assertThat(candidate.trackNoteId()).isEqualTo(trackNoteId);
        assertThat(candidate.spotifyTrackId()).isEqualTo("spotify-track-1");
        assertThat(candidate.track()).isEqualTo("Spunky");
        assertThat(candidate.album()).isEqualTo("Spunky");
        assertThat(candidate.artist()).isEqualTo("Monty Alexander");
        assertThat(candidate.logNumber()).isEqualTo(1);
        assertThat(candidate.decisionContext().jazzLogsCaptionEssence()).isEqualTo("JazzLogs post caption essence");
        assertThat(candidate.decisionContext().vibe()).containsExactly("playful", "buoyant");
        assertThat(candidate.decisionContext().standoutTags()).containsExactly("swing", "late-night");
        assertThat(candidate.deliveryMetadata().spotifyUrl()).isEqualTo("https://open.spotify.com/track/spotify-track-1");
        assertThat(candidate.deliveryMetadata().coverImageUrl()).isEqualTo("https://i.scdn.co/image/spunky");
    }

    private TrackNote buildTrackNote(UUID id) throws Exception {
        var trackNote = TrackNote.create(new TrackNoteData(
                "spotify-track-1",
                "spotify-album-1",
                1,
                "Spunky",
                "Spunky",
                "artist-1",
                "esencial",
                true,
                true,
                new String[]{"playful", "buoyant"},
                "medium",
                "medium",
                "high",
                "moving",
                "swing",
                "closer",
                "standard",
                "Late set with motion",
                new String[]{"night", "drive"},
                "Why it hits",
                "Editorial note",
                "Recommended if",
                "Avoid if",
                "tenor sax",
                "",
                new String[]{"swing", "late-night"}
        ));
        setField(trackNote, "id", id);
        return trackNote;
    }

    private AlbumLog buildAlbumLogForTrackContext() {
        return AlbumLog.create(new AlbumLogData(
                1,
                "Spunky",
                List.of(new com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogMainArtist("artist-1", "Monty Alexander")),
                "JazzLogs post caption essence",
                LocalDate.of(2026, 4, 1),
                "https://instagram.com/p/spunky",
                "post-bop",
                "instrumental",
                "1965",
                new String[]{"playful", "warm"},
                "esencial",
                new String[]{"buoyant", "night-drive"},
                "medium",
                "medium",
                "high",
                new AlbumLogBestMoment(null, List.of(), null),
                new String[]{"night", "drive"},
                "Extra notes",
                "Why it matters",
                "Editorial note",
                "Recommended if",
                "Avoid if",
                "Album context",
                List.of(),
                "spotify-seed-id"
        ));
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
