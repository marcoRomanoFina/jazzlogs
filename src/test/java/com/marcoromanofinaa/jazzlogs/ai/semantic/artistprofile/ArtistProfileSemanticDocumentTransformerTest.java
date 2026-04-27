package com.marcoromanofinaa.jazzlogs.ai.semantic.artistprofile;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.ai.semantic.core.SemanticDocumentType;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfile;
import com.marcoromanofinaa.jazzlogs.logbook.artistprofile.ArtistProfileData;
import java.lang.reflect.Field;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ArtistProfileSemanticDocumentTransformerTest {

    private final ArtistProfileSemanticDocumentTransformer transformer = new ArtistProfileSemanticDocumentTransformer();

    @Test
    void transformsArtistProfileIntoSemanticDocument() {
        var artistProfile = ArtistProfile.create(new ArtistProfileData(
                "spotify-artist-1",
                "Lee Morgan",
                "trumpet",
                new String[]{"hard_bop", "soul_jazz"},
                "Bright, cutting trumpet lines.",
                "A key hard bop voice.",
                "Pure fire when the arrangement opens space.",
                "The Sidewinder",
                new String[]{"energy", "hard_bop_discovery"},
                "Avoid if you want very soft background jazz.",
                new String[]{"Freddie Hubbard"},
                "Essential hard bop trumpeter.",
                new Integer[]{1, 2}
        ));
        setId(artistProfile, UUID.fromString("33333333-3333-3333-3333-333333333333"));

        var document = transformer.transform(artistProfile);

        assertThat(document.type()).isEqualTo(SemanticDocumentType.ARTIST_PROFILE);
        assertThat(document.getSourceId()).isEqualTo("33333333-3333-3333-3333-333333333333");
        assertThat(document.getTitle()).isEqualTo("Lee Morgan");
        assertThat(document.getEmbeddingText())
                .contains("Lee Morgan es un perfil de artista de JazzLogs centrado en trumpet")
                .contains("estilos principales como hard_bop y soul_jazz")
                .contains("Mirada JazzLogs: Pure fire when the arrangement opens space.")
                .contains("Artistas relacionados incluyen Freddie Hubbard.");
    }

    private void setId(ArtistProfile artistProfile, UUID id) {
        try {
            Field field = ArtistProfile.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(artistProfile, id);
        } catch (ReflectiveOperationException exception) {
            throw new RuntimeException(exception);
        }
    }
}
