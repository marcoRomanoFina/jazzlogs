package com.marcoromanofinaa.jazzlogs.admin.spotify.playlist;

import com.marcoromanofinaa.jazzlogs.admin.AdminAccessRequiredException;
import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.SpotifyPlaylistSyncService;
import com.marcoromanofinaa.jazzlogs.user.model.UserRole;
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
        requireAdmin(authenticatedUser);
        spotifyPlaylistSyncService.syncOfficialPlaylist(
                authenticatedUser.id()
        );

        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(AuthenticatedUser authenticatedUser) {
        if (authenticatedUser == null || authenticatedUser.role() == null) {
            throw new AdminAccessRequiredException();
        }

        if (!UserRole.ADMIN.name().equalsIgnoreCase(authenticatedUser.role())) {
            throw new AdminAccessRequiredException();
        }
    }
}
