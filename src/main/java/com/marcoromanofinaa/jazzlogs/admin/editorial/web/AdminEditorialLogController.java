package com.marcoromanofinaa.jazzlogs.admin.editorial.web;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto.UpsertAlbumLogRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.dto.UpsertArtistLogRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.service.AdminEditorialLogService;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.dto.UpsertTrackLogRequestDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.marcoromanofinaa.jazzlogs.auth.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/v1/admin/editorial/logs")
@RequiredArgsConstructor
public class AdminEditorialLogController {

    private final AdminEditorialLogService editorialLogService;

    @PutMapping("/albums")
    public ResponseEntity<Void> upsertAlbumLog(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertAlbumLogRequestDTO request
    ) {
        editorialLogService.upsertAlbumLog(
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/tracks")
    public ResponseEntity<Void> upsertTrackLog(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertTrackLogRequestDTO request
    ) {
        editorialLogService.upsertTrackLog(
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/artists")
    public ResponseEntity<Void> upsertArtistLog(
            @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
            @Valid @RequestBody UpsertArtistLogRequestDTO request
    ) {
        editorialLogService.upsertArtistLog(
                authenticatedUser.id(),
                request
        );

        return ResponseEntity.noContent().build();
    }
}
