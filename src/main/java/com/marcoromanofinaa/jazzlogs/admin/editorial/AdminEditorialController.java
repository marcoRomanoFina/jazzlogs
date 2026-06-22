package com.marcoromanofinaa.jazzlogs.admin.editorial;

import com.marcoromanofinaa.jazzlogs.admin.AdminAccessRequiredException;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.UpsertAlbumEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.UpsertArtistEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.UpsertTrackEditorialRequestDTO;
import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;
import com.marcoromanofinaa.jazzlogs.user.model.UserRole;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/editorial")
@RequiredArgsConstructor
public class AdminEditorialController {

    private final AdminEditorialService editorialService;

    @PutMapping("/albums")
    public ResponseEntity<Void> upsertAlbumEditorial(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertAlbumEditorialRequestDTO request
    ) {
        requireAdmin(authenticatedUser);
        editorialService.upsertAlbumEditorial(authenticatedUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/tracks")
    public ResponseEntity<Void> upsertTrackEditorial(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertTrackEditorialRequestDTO request
    ) {
        requireAdmin(authenticatedUser);
        editorialService.upsertTrackEditorial(authenticatedUser.id(), request);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/artists")
    public ResponseEntity<Void> upsertArtistEditorial(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertArtistEditorialRequestDTO request
    ) {
        requireAdmin(authenticatedUser);
        editorialService.upsertArtistEditorial(authenticatedUser.id(), request);
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
