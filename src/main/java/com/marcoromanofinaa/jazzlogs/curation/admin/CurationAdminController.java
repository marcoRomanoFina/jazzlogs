package com.marcoromanofinaa.jazzlogs.curation.admin;

import com.marcoromanofinaa.jazzlogs.curation.albumlog.AlbumLogCurationUpsertHandler;
import com.marcoromanofinaa.jazzlogs.curation.artistprofile.ArtistProfileCurationUpsertHandler;
import com.marcoromanofinaa.jazzlogs.curation.tracknote.TrackNoteCurationUpsertHandler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/curation")
@Slf4j
@RequiredArgsConstructor
public class CurationAdminController {

    private static final String ADMIN_HEADER = "X-Admin-Key";

    private final AdminRequestAuthorizer authorizer;
    private final AlbumLogCurationUpsertHandler albumLogCurationUpsertHandler;
    private final TrackNoteCurationUpsertHandler trackNoteCurationUpsertHandler;
    private final ArtistProfileCurationUpsertHandler artistProfileCurationUpsertHandler;

    @PostMapping("/album-logs")
    public CurationUpsertResponse upsertAlbumLog(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody UpsertAlbumLogRequest request
    ) {
        authorizer.authorize(adminKey);
        log.info("Admin requested curation upsert for album log {}", request.logNumber());
        var result = albumLogCurationUpsertHandler.upsert(request);
        return new CurationUpsertResponse(result.type().name(), result.identifier(), result.upsertedCount());
    }

    @PostMapping("/track-notes")
    public CurationUpsertResponse upsertTrackNote(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody UpsertTrackNoteRequest request
    ) {
        authorizer.authorize(adminKey);
        log.info("Admin requested curation upsert for track note spotifyTrackId={}", request.spotifyTrackId());
        var result = trackNoteCurationUpsertHandler.upsert(request);
        return new CurationUpsertResponse(result.type().name(), result.identifier(), result.upsertedCount());
    }

    @PostMapping("/artist-profiles")
    public CurationUpsertResponse upsertArtistProfile(
            @RequestHeader(ADMIN_HEADER) String adminKey,
            @Valid @RequestBody UpsertArtistProfileRequest request
    ) {
        authorizer.authorize(adminKey);
        log.info("Admin requested curation upsert for artist profile spotifyArtistId={}", request.spotifyArtistId());
        var result = artistProfileCurationUpsertHandler.upsert(request);
        return new CurationUpsertResponse(result.type().name(), result.identifier(), result.upsertedCount());
    }
}
