package com.marcoromanofinaa.jazzlogs.user.mapper;

import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import com.marcoromanofinaa.jazzlogs.user.subscription.model.UserSubscription;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDto toDTO(User user, boolean hasPreferences) {
        return toDTO(user, user.getSubscription(), hasPreferences);
    }

    public UserDto toDTO(User user, UserSubscription subscription, boolean hasPreferences) {
        return new UserDto(
                user.getId(),
                user.getDisplayName(),
                user.getEmail(),
                subscription == null ? null : subscription.getPlan(),
                user.getRole(),
                hasPreferences
        );
    }
}
