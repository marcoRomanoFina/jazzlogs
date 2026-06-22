package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto;

import java.util.List;

public record UserJazzPreferencesDto(
        String jazzExperienceLevel,
        List<String> favoriteArtists,
        List<String> preferredSubgenres,
        List<String> preferredMoods,
        List<String> favoriteInstruments,
        String tempoFeel,
        boolean likesVocals,
        String discoveryMode
) {
    public List<String> favoriteArtistLabels() {
        return favoriteArtists == null ? List.of() : favoriteArtists;
    }

    public List<String> preferredSubgenreLabels() {
        return preferredSubgenres == null ? List.of() : preferredSubgenres;
    }

    public List<String> preferredMoodLabels() {
        return preferredMoods == null ? List.of() : preferredMoods;
    }

    public List<String> favoriteInstrumentLabels() {
        return favoriteInstruments == null ? List.of() : favoriteInstruments;
    }

    public String jazzExperienceLevelLabel() {
        return jazzExperienceLevel;
    }

    public String tempoFeelLabel() {
        return tempoFeel;
    }

    public String discoveryModeLabel() {
        return discoveryMode;
    }
}
