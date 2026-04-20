package com.marcoromanofinaa.jazzlogs.logbook.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumLogPersonnel(
        @NotBlank
        @Size(max = 255)
        String name,
        @NotBlank
        @Size(max = 100)
        String role
) {
}
