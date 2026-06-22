package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserJazzPreferencesRequestDto(
        @NotBlank String jazzExperienceLevel,
        @NotNull @Size(max = 5) List<@NotBlank String> favoriteArtists,
        @NotEmpty @Size(max = 3) List<@NotBlank String> preferredSubgenres,
        @NotEmpty @Size(max = 3) List<@NotBlank String> preferredMoods,
        @NotNull @Size(max = 3) List<@NotBlank String> favoriteInstruments,
        @NotBlank String tempoFeel,
        boolean likesVocals,
        @NotBlank String discoveryMode
) {
}
