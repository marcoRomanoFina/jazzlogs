package com.marcoromanofinaa.jazzlogs.user.dto;

import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesDto;

public record UserProfileDto(
        UserDto user,
        UserJazzPreferencesDto preferences
) {}
