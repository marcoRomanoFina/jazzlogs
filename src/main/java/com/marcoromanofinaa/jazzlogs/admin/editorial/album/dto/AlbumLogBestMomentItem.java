package com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumLogBestMomentItem(

        @JsonProperty("momento")
        @NotBlank
        @Size(max = 255)
        String momento,

        @JsonProperty("descripción")
        @NotBlank
        @Size(max = 2000)
        String descripcion
) {
}
