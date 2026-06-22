package com.marcoromanofinaa.jazzlogs.admin.editorial.album;

import jakarta.validation.constraints.NotBlank;

public record AlbumEditorialMainArtist(

        @NotBlank
        String name,

        @NotBlank
        String spotifyArtistId
) {
}
