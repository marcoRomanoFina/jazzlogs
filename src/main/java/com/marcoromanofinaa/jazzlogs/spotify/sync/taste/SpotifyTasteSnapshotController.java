package com.marcoromanofinaa.jazzlogs.spotify.sync.taste;

import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/spotify/taste-snapshot")
@RequiredArgsConstructor
public class SpotifyTasteSnapshotController {

    private final SpotifyTasteSyncService spotifyTasteSyncService;

    @PostMapping("/sync")
    public ResponseEntity<Void> syncTasteProfile(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser
    ) {
        spotifyTasteSyncService.syncTasteProfile(authenticatedUser.id());
        return ResponseEntity.noContent().build();
    }
}
