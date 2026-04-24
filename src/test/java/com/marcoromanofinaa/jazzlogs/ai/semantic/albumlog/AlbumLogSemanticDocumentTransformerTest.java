package com.marcoromanofinaa.jazzlogs.ai.semantic.albumlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogPersonnel;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlbumLogSemanticDocumentTransformerTest {

    private final AlbumLogSemanticDocumentTransformer transformer = new AlbumLogSemanticDocumentTransformer();

    @Test
    void transformsAlbumLogIntoSemanticDocument() {
        var albumLog = AlbumLog.create(new AlbumLogData(
                1,
                "Moanin'",
                "Art Blakey & The Jazz Messengers",
                "Original caption",
                LocalDate.of(2026, 4, 15),
                "https://www.instagram.com/p/TEST123/",
                "Hard Bop",
                "1958",
                new String[]{"soulful", "groovy"},
                "essential",
                new String[]{"late-night", "warm"},
                "medium-high",
                "deep",
                "accessible",
                "The title track locks into a churchy groove.",
                new String[]{"rainy-night", "jazz-discovery"},
                "Personal log note.",
                "It defines the hard bop sound.",
                "A direct and soulful gateway into hard bop.",
                "You want bluesy, driving jazz.",
                "You need background-only music.",
                "Recorded at a peak moment for the Jazz Messengers.",
                List.of(new AlbumLogPersonnel("Lee Morgan", "trumpet")),
                "spotify-album-1"
        ));

        var document = transformer.transform(albumLog);

        assertThat(document.type()).isEqualTo(SemanticDocumentType.ALBUM_LOG);
        assertThat(document.getSourceId()).isEqualTo("album-log-1");
        assertThat(document.getTitle()).isEqualTo("Moanin' by Art Blakey & The Jazz Messengers");
        assertThat(document.getEmbeddingText())
                .contains("Moanin' de Art Blakey & The Jazz Messengers es un álbum 1958, Hard Bop y essential")
                .contains("Su carácter musical tiene medium-high de energía, deep de intensidad emocional y un nivel de accesibilidad accessible.")
                .contains("Crea una atmósfera late-night y warm, con moods como soulful y groovy.")
                .contains("Recomendado si: You want bluesy, driving jazz.")
                .contains("Lee Morgan en trumpet");
    }
}
