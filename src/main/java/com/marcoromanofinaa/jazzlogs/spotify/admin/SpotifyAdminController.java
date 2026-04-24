package com.marcoromanofinaa.jazzlogs.spotify.admin;

import com.marcoromanofinaa.jazzlogs.curation.admin.AdminApiProperties;
import com.marcoromanofinaa.jazzlogs.core.exception.AdminApiKeyNotConfiguredException;
import com.marcoromanofinaa.jazzlogs.core.exception.InvalidAdminApiKeyException;
import com.marcoromanofinaa.jazzlogs.spotify.auth.SpotifyConnectionService;
import com.marcoromanofinaa.jazzlogs.spotify.playlist.SpotifyPlaylistSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/spotify")
@Slf4j
@RequiredArgsConstructor
public class SpotifyAdminController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final SpotifyConnectionService spotifyConnectionService;
    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;
    private final AdminApiProperties adminProperties;

    @PostMapping("/authorization-url")
    public SpotifyAuthorizationUrlResponse createAuthorizationUrl(@RequestHeader(ADMIN_HEADER) String adminKey) {
        authorize(adminKey);
        log.info("Admin requested Spotify authorization URL");
        return new SpotifyAuthorizationUrlResponse(spotifyConnectionService.createAuthorizationUrl());
    }

    @GetMapping("/callback")
    public SpotifyCallbackResponse callback(
            @RequestParam String code,
            @RequestParam String state
    ) {
        log.info("Received Spotify callback");
        spotifyConnectionService.handleAuthorizationCallback(code, state);
        return new SpotifyCallbackResponse("Spotify connection stored successfully. You can close this tab.");
    }

    @PostMapping("/playlist-items/sync")
    public SpotifyPlaylistSyncResponse syncPlaylistItems(@RequestHeader(ADMIN_HEADER) String adminKey) {
        authorize(adminKey);
        log.info("Admin requested Spotify playlist sync");
        var syncedCount = spotifyPlaylistSyncService.syncConfiguredPlaylist();
        log.info("Admin Spotify playlist sync finished with {} items", syncedCount);
        return new SpotifyPlaylistSyncResponse(syncedCount);
    }

    private void authorize(String adminKey) {
        if (adminProperties.apiKey() == null || adminProperties.apiKey().isBlank()) {
            throw new AdminApiKeyNotConfiguredException();
        }

        if (!adminProperties.apiKey().equals(adminKey)) {
            log.warn("Rejected admin Spotify request due to invalid admin API key");
            throw new InvalidAdminApiKeyException();
        }
    }
}
