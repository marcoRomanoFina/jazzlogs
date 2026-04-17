package com.marcoromanofinaa.jazzlogs.ingestion.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record AlbumLogSeed(
        @NotBlank
        @Size(max = 255)
        String album,
        @NotBlank
        @Size(max = 255)
        String artist,
        @NotBlank
        @Size(max = 20000)
        String caption,
        @NotNull LocalDate postedAt,
        @NotBlank
        @Size(max = 512)
        @Pattern(
                regexp = "^https://www\\.instagram\\.com/p/[A-Za-z0-9_-]+/?(?:\\?.*)?$",
                message = "instagramPermalink must be a valid Instagram post URL"
        )
        String instagramPermalink,
        @Size(max = 255)
        String style,
        @NotNull
        @Positive
        Integer logNumber,
        @NotNull
        @NotEmpty
        @Size(max = 10)
        List<@NotBlank @Size(max = 50) String> moods,
        @Size(max = 1000)
        String notes,
        @Size(max = 64)
        String spotifyAlbumId
) {
}
