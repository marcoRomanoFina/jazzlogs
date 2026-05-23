package com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto;

import jakarta.validation.constraints.NotBlank;

public record AlbumLogMainArtist(

        @NotBlank
        String name,

        @NotBlank
        String spotifyArtistId
) {
}