package com.marcoromanofinaa.jazzlogs.admin.editorial.artist.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertArtistLogRequestDTO(

        @NotNull
        @Valid
        ArtistLogData artistData
) {
}
