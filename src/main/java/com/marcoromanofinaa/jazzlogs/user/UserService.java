package com.marcoromanofinaa.jazzlogs.user;

import com.marcoromanofinaa.jazzlogs.user.dto.UserProfileDto;
import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.mapper.UserJazzPreferencesMapper;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.repository.UserJazzPreferencesRepository;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserJazzPreferencesRepository userJazzPreferencesRepository;
    private final UserJazzPreferencesMapper userJazzPreferencesMapper;
    private final UserMapper userMapper;

    @Transactional(readOnly = true)
    public UserDto getUserById(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        var hasPreferences = userJazzPreferencesRepository.findByUserId(userId).isPresent();
        return userMapper.toDTO(user, hasPreferences);
    }

    @Transactional(readOnly = true)
    public UserProfileDto getProfile(UUID userId) {
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        var preferences = userJazzPreferencesRepository.findByUserId(userId);
        var hasPreferences = preferences.isPresent();

        return new UserProfileDto(
                userMapper.toDTO(user, hasPreferences),
                preferences.flatMap(userJazzPreferencesMapper::toDTO).orElse(null)
        );
    }
}
