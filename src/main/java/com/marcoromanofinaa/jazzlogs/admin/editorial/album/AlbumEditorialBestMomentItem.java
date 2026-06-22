package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.EditorialVocabularyType;
import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.ValuesFromVocabulary;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record AlbumEditorialBestMomentItem(

        @NotEmpty
        @Size(max = 3)
        @ValuesFromVocabulary(EditorialVocabularyType.CONTEXT)
        List<@NotBlank @Size(max = 100) String> contexts,

        @NotBlank
        @Size(max = 2000)
        String description
) {
}
