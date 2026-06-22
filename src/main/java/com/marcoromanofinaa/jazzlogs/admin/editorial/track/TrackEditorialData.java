package com.marcoromanofinaa.jazzlogs.admin.editorial.track;

import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.EditorialVocabularyType;
import com.marcoromanofinaa.jazzlogs.editorial.vocabulary.ValuesFromVocabulary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record TrackEditorialData(

        @NotBlank
        @Size(max = 64)
        String spotifyTrackId,

        @Size(max = 64)
        String spotifyAlbumId,

        Integer logNumber,

        @NotBlank
        @Size(max = 512)
        String trackName,

        @NotBlank
        @Size(max = 512)
        String albumName,

        @Size(max = 512)
        String primaryArtist,

        @Size(max = 64)
        String mainArtistSpotifyId,

        @Size(max = 128)
        String tier,

        @Size(max = 255)
        String vocalProfile,

        @NotNull
        Boolean standout,

        @NotEmpty
        @ValuesFromVocabulary(EditorialVocabularyType.MOOD)
        List<@NotBlank @Size(max = 255) String> moods,

        @Size(max = 128)
        String energy,

        @Size(max = 128)
        String moodIntensity,

        @Size(max = 128)
        String accessibility,

        @Size(max = 128)
        String tempoFeel,

        @Size(max = 128)
        @ValuesFromVocabulary(EditorialVocabularyType.RHYTHM)
        String rhythmFeel,

        @Size(max = 255)
        String albumRole,

        @Size(max = 255)
        String compositionType,

        String bestMoment,

        @NotEmpty
        @ValuesFromVocabulary(EditorialVocabularyType.CONTEXT)
        List<@NotBlank @Size(max = 255) String> listeningContext,

        String whyItHits,

        String editorialNote,

        String recommendedIf,

        String avoidIf,

        @NotEmpty
        @ValuesFromVocabulary(EditorialVocabularyType.INSTRUMENT)
        List<@NotBlank @Size(max = 255) String> instruments,

        @Size(max = 255)
        String vocalStyle
) {
}
