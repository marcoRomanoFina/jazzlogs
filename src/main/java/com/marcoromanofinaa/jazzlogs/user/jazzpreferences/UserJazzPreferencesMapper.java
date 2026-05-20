package com.marcoromanofinaa.jazzlogs.user.jazzpreferences;

import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserJazzPreferencesMapper {

    public Optional<UserJazzPreferencesDto> toDTO(UserJazzPreferences preferences) {
        if (preferences == null) {
            return Optional.empty();
        }

        return Optional.of(new UserJazzPreferencesDto(
                preferences.getJazzExperienceLevel(),
                preferences.getFavoriteArtists(),
                preferences.getPreferredSubgenres(),
                preferences.getPreferredMoods(),
                preferences.getFavoriteInstruments(),
                preferences.getTempoFeel(),
                preferences.isLikesVocals(),
                preferences.getDiscoveryMode()
        ));
    }
}
