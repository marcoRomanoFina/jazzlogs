package com.marcoromanofinaa.jazzlogs.curation.admin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UpsertTrackNoteRequest(
        @NotBlank
        @Size(max = 64)
        String spotifyTrackId,
        @Size(max = 64)
        String spotifyAlbumId,
        @NotNull
        @Positive
        Integer logNumber,
        @NotBlank
        @Size(max = 512)
        String track,
        @NotBlank
        @Size(max = 512)
        String album,
        @NotBlank
        @Size(max = 64)
        String artistId,
        @Size(max = 64)
        String tier,
        boolean isInstrumental,
        boolean isStandout,
        @NotNull
        @Size(max = 10)
        List<@NotBlank @Size(max = 50) String> vibe,
        @Size(max = 32)
        String energy,
        @Size(max = 32)
        String moodIntensity,
        @Size(max = 32)
        String accessibility,
        @Size(max = 32)
        String tempoFeel,
        @Size(max = 64)
        String rhythmicFeel,
        @Size(max = 64)
        String trackRole,
        @Size(max = 64)
        String compositionType,
        @Size(max = 1000)
        String bestMoment,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 50) String> listeningContext,
        @Size(max = 4000)
        String whyItHits,
        @Size(max = 4000)
        String editorialNote,
        @Size(max = 2000)
        String recommendedIf,
        @Size(max = 2000)
        String avoidIf,
        @Size(max = 128)
        String instrumentFocus,
        @Size(max = 128)
        String vocalStyle,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 50) String> standoutTags
) {
    public UpsertTrackNoteRequest {
        vibe = vibe == null ? List.of() : vibe;
        listeningContext = listeningContext == null ? List.of() : listeningContext;
        standoutTags = standoutTags == null ? List.of() : standoutTags;
    }
}
