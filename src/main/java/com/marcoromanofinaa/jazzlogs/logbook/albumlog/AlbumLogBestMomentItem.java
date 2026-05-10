package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumLogBestMomentItem(
        @NotBlank
        @Size(max = 255)
        String momento,
        @NotBlank
        @Size(max = 2000)
        String descripcion
) {
}
