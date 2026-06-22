package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.mapper;

import com.marcoromanofinaa.jazzlogs.editorial.graph.model.node.UserNode;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.DiscoveryMode;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.JazzExperienceLevel;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.TempoFeel;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class UserJazzPreferencesMapper {

    public Optional<UserJazzPreferencesDto> toDTO(UserNode preferences) {
        if (preferences == null
                || preferences.getJazzExperienceLevel() == null
                || preferences.getDiscoveryMode() == null
                || preferences.getLikesVocals() == null
                || preferences.getPreferredTempoFeel() == null) {
            return Optional.empty();
        }

        return Optional.of(new UserJazzPreferencesDto(
                enumLabel(JazzExperienceLevel.valueOf(preferences.getJazzExperienceLevel())),
                preferences.getFavoriteArtists().stream()
                        .map(artist -> artist.getName())
                        .toList(),
                preferences.getPreferredStyles().stream()
                        .map(style -> style.getName())
                        .toList(),
                preferences.getPreferredMoods().stream()
                        .map(mood -> mood.getName())
                        .toList(),
                preferences.getFavoriteInstruments().stream()
                        .map(instrument -> instrument.getName())
                        .toList(),
                enumLabel(TempoFeel.valueOf(preferences.getPreferredTempoFeel())),
                preferences.getLikesVocals(),
                enumLabel(DiscoveryMode.valueOf(preferences.getDiscoveryMode()))
        ));
    }

    private String enumLabel(Enum<?> value) {
        return switch (value) {
            case JazzExperienceLevel level -> level.label();
            case TempoFeel feel -> feel.label();
            case DiscoveryMode mode -> mode.label();
            default -> value.name();
        };
    }
}
