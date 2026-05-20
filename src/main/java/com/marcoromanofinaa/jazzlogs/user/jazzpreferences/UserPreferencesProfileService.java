package com.marcoromanofinaa.jazzlogs.user.jazzpreferences;

import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserPreferencesProfileService {

    private final UserRepository userRepository;
    private final UserJazzPreferencesRepository userJazzPreferencesRepository;

    @Transactional
    public UserDto upsertPreferences(UUID userId, UserJazzPreferencesDto userPreferences) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        var preferences = userJazzPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> new UserJazzPreferences(userId));

        applyPreferences(preferences, userPreferences);
        userJazzPreferencesRepository.save(preferences);

        return user.toDto(true);
    }

    private void applyPreferences(UserJazzPreferences preferences, UserJazzPreferencesDto dto) {
        preferences.apply(
                dto.jazzExperienceLevel(),
                dto.favoriteArtists(),
                dto.preferredSubgenres(),
                dto.preferredMoods(),
                dto.favoriteInstruments(),
                dto.tempoFeel(),
                dto.likesVocals(),
                dto.discoveryMode()
        );
    }
}
