package com.marcoromanofinaa.jazzlogs.admin.editorial.track.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record UpsertTrackLogRequestDTO(

        @NotNull
        @Valid
        TrackLogData trackData
) {
}
