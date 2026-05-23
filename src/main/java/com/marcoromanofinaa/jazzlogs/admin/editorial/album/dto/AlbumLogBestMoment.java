package com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AlbumLogBestMoment(

        @JsonProperty("introducción")
        @NotBlank
        String introduccion,

        @JsonProperty("momentos")
        @Size(max = 3)
        @Valid
        List<AlbumLogBestMomentItem> momentos,

        @JsonProperty("conclusión")
        @NotBlank
        String conclusion
) {
}