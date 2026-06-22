package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.EditorialVocabularyType;
import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.ValuesFromVocabulary;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import org.hibernate.validator.constraints.URL;

public record AlbumEditorialData(

        @NotNull
        @Positive
        Integer logNumber,

        @NotBlank
        @Size(max = 200)
        String albumName,

        @NotEmpty
        @Valid
        List<AlbumEditorialMainArtist> mainArtists,

        @Size(max = 5000)
        String captionEssence,

        @NotNull
        @PastOrPresent
        LocalDate postedAt,

        @URL
        @Size(max = 500)
        String instagramPermalink,

        @NotEmpty
        @Size(max = 10)
        @ValuesFromVocabulary(EditorialVocabularyType.STYLE)
        List<@NotBlank @Size(max = 100) String> styles,

        @NotBlank
        @Size(max = 100)
        String vocalProfile,

        @NotNull
        @Min(1900)
        Integer releaseYear,

        @NotEmpty
        @ValuesFromVocabulary(EditorialVocabularyType.MOOD)
        @Size(max = 10)
        List<@NotBlank @Size(max = 100) String> moods,

        @NotBlank
        @Size(max = 50)
        String tier,

        @NotBlank
        String energy,

        @NotBlank
        String moodIntensity,

        @NotBlank
        String accessibility,

        @Valid
        AlbumEditorialBestMoment bestMoment,

        @NotEmpty
        @Size(max = 10)
        @ValuesFromVocabulary(EditorialVocabularyType.CONTEXT)
        List<@NotBlank @Size(max = 100) String> listeningContext,

        @NotBlank
        @Size(max = 3000)
        String whyItMatters,

        @NotBlank
        @Size(max = 5000)
        String editorialNote,

        @NotBlank
        @Size(max = 2000)
        String recommendedIf,

        @NotBlank
        @Size(max = 2000)
        String avoidIf,

        @NotBlank
        @Size(max = 5000)
        String albumContext,

        @Valid
        List<AlbumEditorialPersonnel> personnel,

        @NotBlank
        String spotifyAlbumId
) {
}
