package com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertAlbumLogRequestDTO(

        @NotNull
        @Valid
        AlbumLogData albumData
) {
}
