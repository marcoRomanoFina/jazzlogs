package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.EditorialVocabularyType;
import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.ValuesFromVocabulary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlbumEditorialPersonnel(

        @NotBlank
        @Size(max = 255)
        String name,

        @Size(max = 255)
        String spotifyArtistId,

        @NotEmpty
        @Size(max = 8)
        @ValuesFromVocabulary(EditorialVocabularyType.INSTRUMENT)
        List<@NotBlank @Size(max = 100) String> instruments
) {
}
