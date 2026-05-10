package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumLogPersonnel(
        @Size(max = 64)
        String spotifyArtistId,
        @NotBlank
        @Size(max = 255)
        String name,
        @NotBlank
        @Size(max = 100)
        String role
) {
}
