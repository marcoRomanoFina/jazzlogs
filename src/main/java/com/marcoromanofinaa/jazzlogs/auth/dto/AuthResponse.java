package com.marcoromanofinaa.jazzlogs.auth.dto;

import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;

public record AuthResponse(
        String accessToken,
        String tokenType,
        UserDto user
) {
    public AuthResponse(String accessToken, UserDto user) {
        this(accessToken, "Bearer", user);
    }
}
