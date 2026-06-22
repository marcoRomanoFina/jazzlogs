package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertAlbumEditorialRequestDTO(

        @NotNull
        @Valid
        AlbumEditorialData albumData
) {
}
