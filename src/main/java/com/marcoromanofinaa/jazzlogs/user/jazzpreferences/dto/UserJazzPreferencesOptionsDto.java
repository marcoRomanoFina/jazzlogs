package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto;

import java.util.List;

public record UserJazzPreferencesOptionsDto(
        List<String> favoriteArtistOptions,
        List<String> preferredSubgenreOptions,
        List<String> preferredMoodOptions,
        List<String> favoriteInstrumentOptions,
        List<String> tempoFeelOptions,
        List<String> discoveryModeOptions,
        List<String> jazzExperienceLevelOptions
) {
}
