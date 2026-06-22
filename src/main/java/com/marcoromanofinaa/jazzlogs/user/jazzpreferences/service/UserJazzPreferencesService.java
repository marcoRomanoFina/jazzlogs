package com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service;

import com.marcoromanofinaa.jazzlogs.core.exception.FeatureUnavailableException;
import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesOptionsDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesRequestDto;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserJazzPreferencesService {

    private final UserRepository userRepository;
    private final UserGraphPreferencesService userGraphPreferencesService;
    private final UserMapper userMapper;
    private final UserSubscriptionService userSubscriptionService;

    @Transactional(readOnly = true)
    public UserJazzPreferencesOptionsDto getPreferencesOptions() {
        var options = userGraphPreferencesService.getPreferencesOptions();
        if (options.favoriteArtistOptions().isEmpty()
                || options.preferredSubgenreOptions().isEmpty()
                || options.preferredMoodOptions().isEmpty()
                || options.favoriteInstrumentOptions().isEmpty()) {
            throw new FeatureUnavailableException(
                    "Jazz preference options are not available yet. Load the editorial graph vocabulary first."
            );
        }
        return options;
    }

    @Transactional
    public UserDto upsertPreferences(UUID userId, UserJazzPreferencesRequestDto userPreferences) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        userGraphPreferencesService.upsertPreferences(user, userPreferences);
        var subscription = userSubscriptionService.getCurrentSubscription(userId);
        return userMapper.toDTO(user, subscription, true);
    }
}
