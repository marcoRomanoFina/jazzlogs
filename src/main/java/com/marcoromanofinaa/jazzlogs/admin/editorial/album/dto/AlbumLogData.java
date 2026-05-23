package com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto;

import java.time.LocalDate;
import java.util.List;

import org.hibernate.validator.constraints.URL;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record AlbumLogData(

        @NotNull
        @Positive
        Integer logNumber,

        @NotBlank
        @Size(max = 200)
        String albumName,

        @NotEmpty
        @Valid
        List<AlbumLogMainArtist> mainArtists,

        @Size(max = 5000)
        String captionEssence,

        @NotNull
        @PastOrPresent
        LocalDate postedAt,

        @URL
        @Size(max = 500)
        String instagramPermalink,

        @NotBlank
        @Size(max = 100)
        String style,

        @NotBlank
        @Size(max = 100)
        String vocalProfile,

        @NotNull
        @Min(1900)
        Integer releaseYear,

        @NotEmpty
        @Size(max = 10)
        String[] moods,

        @NotBlank
        @Size(max = 50)
        String tier,

        @NotEmpty
        @Size(max = 10)
        String[] vibe,

        @NotBlank
        String energy,

        @NotBlank
        String moodIntensity,

        @NotBlank
        String accessibility,

        @Valid
        AlbumLogBestMoment bestMoment,

        @NotEmpty
        @Size(max = 10)
        String[] listeningContext,

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
        List<AlbumLogPersonnel> personnel,

        @NotBlank
        String spotifyAlbumId
) {
}
