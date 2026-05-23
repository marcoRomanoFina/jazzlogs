package com.marcoromanofinaa.jazzlogs.admin.spotify.playlist;

import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.SpotifyPlaylistSyncService;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/spotify/playlists")
@RequiredArgsConstructor
public class AdminSpotifyPlaylistController {

    private final SpotifyPlaylistSyncService spotifyPlaylistSyncService;

    @PostMapping("/official/sync")
    public ResponseEntity<Void> syncOfficialJazzlogsPlaylist(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        spotifyPlaylistSyncService.syncOfficialPlaylist(
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }
}