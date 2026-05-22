package com.marcoromanofinaa.jazzlogs.spotify.connection;

import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SpotifyConnectionController {

    private final SpotifyConnectionService spotifyConnectionService;

    @PostMapping("/spotify/authorization-url")
    public ResponseEntity<SpotifyAuthorizationUrlDTO> createAuthorizationUrl(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody SpotifyAuthorizationRequest request
    ) {
        return ResponseEntity.ok(
                spotifyConnectionService.createAuthorizationUrl(authenticatedUser.id(), request.scopes())
        );
    }

    @GetMapping("/spotify/callback")
    public ResponseEntity<Void> handleCallback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(spotifyConnectionService.handleCallback(code, state))
                .build();
    }
}
