package com.marcoromanofinaa.jazzlogs.curation.artistprofile;

import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertArtistProfileRequest;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationElementType;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertHandler;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArtistProfileCurationUpsertHandler implements CurationUpsertHandler<UpsertArtistProfileRequest> {

    private final ArtistProfileCurationService artistProfileCurationService;

    @Override
    public CurationElementType type() {
        return CurationElementType.ARTIST_PROFILE;
    }

    @Override
    public String identifier(UpsertArtistProfileRequest request) {
        return request.spotifyArtistId();
    }

    @Override
    public CurationUpsertResult upsert(UpsertArtistProfileRequest request) {
        var upserted = artistProfileCurationService.upsert(request) ? 1 : 0;
        return new CurationUpsertResult(type(), identifier(request), upserted);
    }
}
