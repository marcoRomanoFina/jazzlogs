package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service;

import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.model.UserJazzPreferences;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.repository.UserJazzPreferencesRepository;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserJazzPreferencesService {

    private final UserRepository userRepository;
    private final UserJazzPreferencesRepository userJazzPreferencesRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserDto upsertPreferences(UUID userId, UserJazzPreferencesDto userPreferences) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        var preferences = userJazzPreferencesRepository.findByUserId(userId)
                .orElseGet(() -> new UserJazzPreferences(userId));

        applyPreferences(preferences, userPreferences);
        userJazzPreferencesRepository.save(preferences);

        return userMapper.toDTO(user, true);
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
