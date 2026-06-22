package com.marcoromanofinaa.jazzlogs.admin.editorial.track;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertTrackEditorialRequestDTO(

        @NotNull
        @Valid
        TrackEditorialData trackData
) {
}
