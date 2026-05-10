package com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMomentItem;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogMainArtist;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogPersonnel;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AlbumLogSemanticDocumentTransformerTest {

    private final AlbumLogSemanticDocumentTransformer transformer = new AlbumLogSemanticDocumentTransformer();

    @Test
    void transformsAlbumLogIntoSemanticDocument() {
        var albumLog = AlbumLog.create(new AlbumLogData(
                1,
                "Moanin'",
                List.of(new AlbumLogMainArtist("artist-1", "Art Blakey & The Jazz Messengers")),
                "Original caption",
                LocalDate.of(2026, 4, 15),
                "https://www.instagram.com/p/TEST123/",
                "Hard Bop",
                "instrumental",
                "1958",
                new String[]{"soulful", "groovy"},
                "essential",
                new String[]{"late-night", "warm"},
                "medium-high",
                "deep",
                "accessible",
                new AlbumLogBestMoment(
                        "The title track locks into a churchy groove.",
                        List.of(new AlbumLogBestMomentItem("El empuje central", "The band sounds locked in and direct.")),
                        "It feels ideal for a soulful late-night listen."
                ),
                new String[]{"rainy-night", "jazz-discovery"},
                "Personal log note.",
                "It defines the hard bop sound.",
                "A direct and soulful gateway into hard bop.",
                "You want bluesy, driving jazz.",
                "You need background-only music.",
                "Recorded at a peak moment for the Jazz Messengers.",
                List.of(new AlbumLogPersonnel(null, "Lee Morgan", "trumpet")),
                "spotify-album-1"
        ));
        setId(albumLog, UUID.fromString("11111111-1111-1111-1111-111111111111"));

        var document = transformer.transform(albumLog);

        assertThat(document.type()).isEqualTo(SemanticDocumentType.ALBUM_LOG);
        assertThat(document.getSourceId()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(document.getTitle()).isEqualTo("Moanin' by Art Blakey & The Jazz Messengers");
        assertThat(document.getEmbeddingText())
                .contains("Moanin' de Art Blakey & The Jazz Messengers es un álbum 1958, Hard Bop, perfil vocal instrumental y essential")
                .contains("Su carácter musical tiene medium-high de energía, deep de intensidad emocional y un nivel de accesibilidad accessible.")
                .contains("Crea una atmósfera late-night y warm, con moods como soulful y groovy.")
                .contains("The title track locks into a churchy groove.")
                .contains("Recomendado si: You want bluesy, driving jazz.")
                .contains("Lee Morgan en trumpet");
    }

    private void setId(AlbumLog albumLog, UUID id) {
        try {
            Field field = AlbumLog.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(albumLog, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
