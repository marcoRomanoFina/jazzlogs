package com.marcoromanofinaa.jazzlogs.curation.admin;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogPersonnel;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record UpsertAlbumLogRequest(
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
        @Size(max = 16)
        String releaseYear,
        @NotNull
        @Positive
        Integer logNumber,
        @NotNull
        @NotEmpty
        @Size(max = 10)
        List<@NotBlank @Size(max = 50) String> moods,
        @Size(max = 64)
        String tier,
        @NotNull
        @Size(max = 10)
        List<@NotBlank @Size(max = 50) String> vibe,
        @Size(max = 32)
        String energy,
        @Size(max = 32)
        String moodIntensity,
        @Size(max = 32)
        String accessibility,
        @Size(max = 1000)
        String bestMoment,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 50) String> listeningContext,
        @Size(max = 1000)
        String notes,
        @Size(max = 4000)
        String whyItMatters,
        @Size(max = 4000)
        String editorialNote,
        @Size(max = 2000)
        String recommendedIf,
        @Size(max = 2000)
        String avoidIf,
        @Size(max = 4000)
        String albumContext,
        @NotNull
        @Size(max = 30)
        List<@Valid AlbumLogPersonnel> personnel,
        @Size(max = 64)
        String spotifyAlbumId
) {
    public UpsertAlbumLogRequest {
        moods = moods == null ? List.of() : moods;
        vibe = vibe == null ? List.of() : vibe;
        listeningContext = listeningContext == null ? List.of() : listeningContext;
        personnel = personnel == null ? List.of() : personnel;
    }
}
