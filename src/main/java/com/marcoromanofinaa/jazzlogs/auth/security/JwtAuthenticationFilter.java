package com.marcoromanofinaa.jazzlogs.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcoromanofinaa.jazzlogs.auth.exception.InvalidJwtException;
import com.marcoromanofinaa.jazzlogs.auth.exception.UserDisabledException;
import com.marcoromanofinaa.jazzlogs.core.exception.ApiErrorResponse;
import com.marcoromanofinaa.jazzlogs.user.model.UserStatus;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        var authorizationHeader = request.getHeader("Authorization");
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            var token = authorizationHeader.substring(7).trim();
            if (!jwtService.isTokenValid(token)) {
                throw new InvalidJwtException();
            }

            var userId = jwtService.extractUserId(token);
            var user = userRepository.findById(userId)
                    .orElseThrow(InvalidJwtException::new);

            if (user.getStatus() == UserStatus.DISABLED) {
                throw new UserDisabledException();
            }

            var principal = new AuthenticatedUser(
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

            var authentication = new UsernamePasswordAuthenticationToken(
                    principal,
                    null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
            );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (InvalidJwtException | UserDisabledException exception) {
            SecurityContextHolder.clearContext();
            response.setStatus(exception instanceof UserDisabledException
                    ? HttpStatus.FORBIDDEN.value()
                    : HttpStatus.UNAUTHORIZED.value());
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(), new ApiErrorResponse(exception.getMessage()));
        }
    }
}
