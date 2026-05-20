package com.marcoromanofinaa.jazzlogs.user.dto;

import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import com.marcoromanofinaa.jazzlogs.user.model.UserRole;
import java.util.UUID;

public record UserDto(
        UUID userId,
        String displayName,
        String email,
        Plan plan,
        UserRole role,
        boolean hasPreferences
) {}
