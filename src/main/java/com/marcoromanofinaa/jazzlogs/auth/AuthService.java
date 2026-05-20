package com.marcoromanofinaa.jazzlogs.auth;

import com.marcoromanofinaa.jazzlogs.auth.dto.AuthResponse;
import com.marcoromanofinaa.jazzlogs.auth.dto.LoginRequest;
import com.marcoromanofinaa.jazzlogs.auth.dto.RegisterRequest;
import com.marcoromanofinaa.jazzlogs.auth.exception.EmailAlreadyInUseException;
import com.marcoromanofinaa.jazzlogs.auth.exception.InvalidCredentialsException;
import com.marcoromanofinaa.jazzlogs.auth.exception.UserDisabledException;
import com.marcoromanofinaa.jazzlogs.auth.security.JwtService;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.repository.UserJazzPreferencesRepository;
import com.marcoromanofinaa.jazzlogs.user.mapper.UserMapper;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import com.marcoromanofinaa.jazzlogs.user.model.UserStatus;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserJazzPreferencesRepository userJazzPreferencesRepository;
    private final UserMapper userMapper;
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
        var token = jwtService.generateAccessToken(savedUser);
        return new AuthResponse(token, userMapper.toDTO(savedUser, false));
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

        var token = jwtService.generateAccessToken(user);
        var hasPreferences = userJazzPreferencesRepository.findByUserId(user.getId()).isPresent();
        return new AuthResponse(token, userMapper.toDTO(user, hasPreferences));
    }
}
