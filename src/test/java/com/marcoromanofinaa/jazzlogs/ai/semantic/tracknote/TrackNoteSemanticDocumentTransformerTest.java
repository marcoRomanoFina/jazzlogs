package com.marcoromanofinaa.jazzlogs.ai.semantic.tracknote;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNote;
import com.marcoromanofinaa.jazzlogs.logbook.tracknote.TrackNoteData;
import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TrackNoteSemanticDocumentTransformerTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Test
    void transformsTrackNoteIntoSemanticDocument() {
        when(albumLogRepository.findByLogNumber(1)).thenReturn(Optional.empty());

        var transformer = new TrackNoteSemanticDocumentTransformer(albumLogRepository);
        var trackNote = TrackNote.create(new TrackNoteData(
                "spotify-track-1",
                "spotify-album-1",
                1,
                "Moanin'",
                "Moanin'",
                "spotify-artist-1",
                "hall_of_fame",
                true,
                true,
                new String[]{"soulful", "churchy"},
                "high",
                "deep",
                "easy",
                "mid-tempo",
                "swing",
                "opener",
                "original",
                "The call-and-response hook.",
                new String[]{"friday-night"},
                "It hits like a sermon with a backbeat.",
                "A perfect thesis statement for the album.",
                "You want an immediate hard bop anthem.",
                "You want something delicate.",
                "piano and horns",
                "",
                new String[]{"groove", "classic"}
        ));
        setId(trackNote, UUID.fromString("22222222-2222-2222-2222-222222222222"));

        var document = transformer.transform(trackNote);

        assertThat(document.type()).isEqualTo(SemanticDocumentType.TRACK_NOTE);
        assertThat(document.getSourceId()).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(document.getTitle()).isEqualTo("Moanin' from Moanin'");
        assertThat(document.getEmbeddingText())
                .contains("Moanin' es un track instrumental del álbum Moanin', conectado al log #1 de JazzLogs.")
                .contains("El track tiene high de energía, mid-tempo como sensación de tempo, swing como sensación rítmica")
                .contains("Se siente soulful y churchy, con deep de intensidad emocional y un nivel de accesibilidad easy.")
                .contains("Por qué pega: It hits like a sermon with a backbeat.")
                .contains("Es un track destacado asociado con groove y classic.");
    }

    private void setId(TrackNote trackNote, UUID id) {
        try {
            Field field = TrackNote.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(trackNote, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
