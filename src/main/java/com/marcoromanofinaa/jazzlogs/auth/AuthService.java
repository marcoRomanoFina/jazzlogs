package com.marcoromanofinaa.jazzlogs.auth;

import com.marcoromanofinaa.jazzlogs.auth.dto.AuthResponse;
import com.marcoromanofinaa.jazzlogs.auth.dto.LoginRequest;
import com.marcoromanofinaa.jazzlogs.auth.dto.RegisterRequest;
import com.marcoromanofinaa.jazzlogs.auth.exception.EmailAlreadyInUseException;
import com.marcoromanofinaa.jazzlogs.auth.exception.InvalidCredentialsException;
import com.marcoromanofinaa.jazzlogs.auth.exception.UserDisabledException;
import com.marcoromanofinaa.jazzlogs.auth.security.JwtService;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service.UserGraphPreferencesService;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import com.marcoromanofinaa.jazzlogs.user.model.UserStatus;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserGraphPreferencesService userGraphPreferencesService;
    private final UserMapper userMapper;
    private final UserSubscriptionService userSubscriptionService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmailIgnoreCase(request.email())) {
            throw new EmailAlreadyInUseException();
        }

        var user = new User(
                request.firstName(),
                request.lastName(),
                request.displayName(),
                request.email(),
                passwordEncoder.encode(request.password())
        );

        var savedUser = userRepository.save(user);
        userGraphPreferencesService.createUserNode(savedUser);
        userSubscriptionService.renewSubscription(savedUser.getId(), Plan.FREE);
        var token = jwtService.generateAccessToken(savedUser);
        var subscription = userSubscriptionService.getCurrentSubscription(savedUser.getId());
        return new AuthResponse(token, userMapper.toDTO(savedUser, subscription, false));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        var user = userRepository.findByEmailIgnoreCase(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UserDisabledException();
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        user.markLoggedIn();
        var token = jwtService.generateAccessToken(user);
        var hasPreferences = userGraphPreferencesService.hasPreferences(user.getId());
        var subscription = userSubscriptionService.getCurrentSubscription(user.getId());
        return new AuthResponse(token, userMapper.toDTO(user, subscription, hasPreferences));
    }
}
