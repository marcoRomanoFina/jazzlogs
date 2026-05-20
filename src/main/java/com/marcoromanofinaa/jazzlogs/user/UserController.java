package com.marcoromanofinaa.jazzlogs.user;

import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import com.marcoromanofinaa.jazzlogs.user.dto.UserProfileDto;
import com.marcoromanofinaa.jazzlogs.user.dto.UserDto;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.PreferencesRequest;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service.UserJazzPreferencesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserJazzPreferencesService userJazzPreferencesService;

    @GetMapping("/me")
    public ResponseEntity<UserDto> getMe(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(userService.getUserById(authUser.id()));
    }

    @GetMapping("/me/profile")
    public ResponseEntity<UserProfileDto> getMyProfile(@AuthenticationPrincipal AuthenticatedUser authUser) {
        return ResponseEntity.ok(userService.getProfile(authUser.id()));
    }

    @PutMapping("/me/preferences")
    public ResponseEntity<UserDto> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser authUser,
            @Valid @RequestBody PreferencesRequest request
    ) {
        return ResponseEntity.ok(userJazzPreferencesService.upsertPreferences(authUser.id(), request.userPreferences()));
    }
}
