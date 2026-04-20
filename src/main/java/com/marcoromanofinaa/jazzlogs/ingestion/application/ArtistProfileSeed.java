package com.marcoromanofinaa.jazzlogs.ingestion.application;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public record ArtistProfileSeed(
        @NotBlank
        @Size(max = 64)
        String spotifyArtistId,
        @NotBlank
        @Size(max = 512)
        String name,
        @Size(max = 128)
        String primaryInstrument,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 64) String> mainStyles,
        @Size(max = 4000)
        String signatureSound,
        @Size(max = 4000)
        String artistContext,
        @Size(max = 4000)
        String jazzlogsTake,
        @Size(max = 512)
        String recommendedEntryPoint,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 120) String> bestFor,
        @Size(max = 2000)
        String avoidIf,
        @NotNull
        @Size(max = 20)
        List<@NotBlank @Size(max = 255) String> relatedArtists,
        @Size(max = 4000)
        String importance,
        @NotNull
        @Size(max = 100)
        List<@Positive Integer> logAppearances
) {
    public ArtistProfileSeed {
        mainStyles = mainStyles == null ? List.of() : mainStyles;
        bestFor = bestFor == null ? List.of() : bestFor;
        relatedArtists = relatedArtists == null ? List.of() : relatedArtists;
        logAppearances = logAppearances == null ? List.of() : logAppearances;
    }

    boolean isTemplate() {
        return spotifyArtistId == null || spotifyArtistId.isBlank();
    }
}
