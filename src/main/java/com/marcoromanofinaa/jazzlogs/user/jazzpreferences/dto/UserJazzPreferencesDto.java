package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto;

import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.Artist;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.DiscoveryMode;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.Instrument;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.JazzExperienceLevel;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.Mood;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.Subgenre;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.TempoFeel;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserJazzPreferencesDto(
        @NotNull JazzExperienceLevel jazzExperienceLevel,
        @NotNull @Size(max = 5) List<Artist> favoriteArtists,
        @NotNull @Size(max = 3) List<Subgenre> preferredSubgenres,
        @NotNull @Size(max = 3) List<Mood> preferredMoods,
        @NotNull @Size(max = 3) List<Instrument> favoriteInstruments,
        @NotNull TempoFeel tempoFeel,
        boolean likesVocals,
        @NotNull DiscoveryMode discoveryMode
) {}
