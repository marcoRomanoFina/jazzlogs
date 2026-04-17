package com.marcoromanofinaa.jazzlogs.spotify.web;

import com.marcoromanofinaa.jazzlogs.ingestion.application.AlbumLogAdminProperties;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyConnectionService;
import com.marcoromanofinaa.jazzlogs.spotify.application.SpotifyPlaylistSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/spotify")
@RequiredArgsConstructor
public class SpotifyAdminController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final SpotifyConnectionService spotifyConnectionService;
    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;
    private final AlbumLogAdminProperties adminProperties;

    @PostMapping("/authorization-url")
    public SpotifyAuthorizationUrlResponse createAuthorizationUrl(@RequestHeader(ADMIN_HEADER) String adminKey) {
        authorize(adminKey);
        return new SpotifyAuthorizationUrlResponse(spotifyConnectionService.createAuthorizationUrl());
    }

    @GetMapping("/callback")
    public SpotifyCallbackResponse callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        spotifyConnectionService.handleAuthorizationCallback(code, state);
        return new SpotifyCallbackResponse("Spotify connection stored successfully. You can close this tab.");
    }

    @PostMapping("/playlist-items/sync")
    public SpotifyPlaylistSyncResponse syncPlaylistItems(@RequestHeader(ADMIN_HEADER) String adminKey) {
        authorize(adminKey);
        var syncedCount = spotifyPlaylistSyncService.syncConfiguredPlaylist();
        return new SpotifyPlaylistSyncResponse(syncedCount);
    }

    private void authorize(String adminKey) {
        if (adminProperties.apiKey() == null || adminProperties.apiKey().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Admin API key is not configured"
            );
        }

        if (!adminProperties.apiKey().equals(adminKey)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Invalid admin key");
        }
    }
}
