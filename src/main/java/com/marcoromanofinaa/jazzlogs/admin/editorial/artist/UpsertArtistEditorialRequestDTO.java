package com.marcoromanofinaa.jazzlogs.admin.editorial.artist;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertArtistEditorialRequestDTO(

        @NotNull
        @Valid
        ArtistEditorialData artistData
) {
}
