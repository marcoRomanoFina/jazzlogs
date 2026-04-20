package com.marcoromanofinaa.jazzlogs.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.TrackNoteRepository;
import jakarta.validation.Validation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackNoteImportServiceTest {

    @Mock
    private TrackNoteRepository trackNoteRepository;

    @TempDir
    Path tempDir;

    @Test
    void importsTrackNotesAndSkipsTemplateEntry() throws Exception {
        var service = new TrackNoteImportService(
                JsonMapper.builder().findAndAddModules().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                trackNoteRepository
        );
        var json = """
                [
                  {
                    "spotifyTrackId": "track-1",
                    "spotifyAlbumId": "album-1",
                    "logNumber": 1,
                    "track": "Test Track",
                    "album": "Test Album",
                    "artistId": "artist-1",
                    "tier": "essential",
                    "isInstrumental": true,
                    "isStandout": true,
                    "vibe": ["warm", "groovy"],
                    "energy": "high",
                    "moodIntensity": "medium",
                    "accessibility": "easy",
                    "tempoFeel": "upbeat",
                    "rhythmicFeel": "swing",
                    "trackRole": "opener",
                    "compositionType": "original",
                    "bestMoment": "Friday night",
                    "listeningContext": ["friday-night"],
                    "whyItHits": "It grooves.",
                    "editorialNote": "Editorial note.",
                    "recommendedIf": "Recommended.",
                    "avoidIf": "Avoid.",
                    "instrumentFocus": "piano",
                    "vocalStyle": "",
                    "standoutTags": ["groovy"]
                  },
                  {
                    "spotifyTrackId": "",
                    "logNumber": 0,
                    "track": "",
                    "album": "",
                    "artistId": "",
                    "vibe": [],
                    "listeningContext": [],
                    "standoutTags": []
                  }
                ]
                """;
        var path = writeSeedFile(json);
        when(trackNoteRepository.findAllBySpotifyTrackIdIn(anyCollection())).thenReturn(List.of());

        var imported = service.importFromJson(path);

        assertThat(imported).isEqualTo(1);
        var captor = ArgumentCaptor.forClass(Iterable.class);
        verify(trackNoteRepository).saveAll(captor.capture());
        var saved = toList(captor.getValue());
        assertThat(saved).singleElement().satisfies(trackNote -> {
            assertThat(trackNote.getSpotifyTrackId()).isEqualTo("track-1");
            assertThat(trackNote.getSpotifyAlbumId()).isEqualTo("album-1");
            assertThat(trackNote.getTrack()).isEqualTo("Test Track");
            assertThat(trackNote.getVibe()).containsExactly("warm", "groovy");
            assertThat(trackNote.getListeningContext()).containsExactly("friday-night");
            assertThat(trackNote.getStandoutTags()).containsExactly("groovy");
            assertThat(trackNote.getWhyItHits()).isEqualTo("It grooves.");
        });
    }

    private Path writeSeedFile(String json) throws Exception {
        var path = tempDir.resolve("track-notes.json");
        Files.writeString(path, json);
        return path;
    }

    private List<TrackNote> toList(Object iterable) {
        var notes = new ArrayList<TrackNote>();
        for (var note : (Iterable<?>) iterable) {
            notes.add((TrackNote) note);
        }
        return notes;
    }
}
