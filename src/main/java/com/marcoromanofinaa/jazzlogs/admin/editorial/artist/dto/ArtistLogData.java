package com.marcoromanofinaa.jazzlogs.admin.editorial.artist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ArtistLogData(

        @NotBlank
        @Size(max = 64)
        String spotifyArtistId,

        @NotBlank
        @Size(max = 512)
        String artistName,

        @Size(max = 255)
        String primaryInstrument,

        @NotEmpty
        List<@NotBlank @Size(max = 255) String> mainStyles,

        String soundProfile,

        String artistContext,

        String editorialNote,

        @Size(max = 255)
        String entryPointLogId,

        @NotEmpty
        List<@NotBlank @Size(max = 255) String> bestListeningMoments,

        String avoidIf,

        List<@NotBlank @Size(max = 255) String> relatedArtists,

        String whyItMatters,

        List<Integer> appearsInLogs
) {
}
