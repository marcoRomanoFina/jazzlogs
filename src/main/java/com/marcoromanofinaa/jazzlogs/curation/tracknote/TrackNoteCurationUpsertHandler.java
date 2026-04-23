package com.marcoromanofinaa.jazzlogs.curation.tracknote;

import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertTrackNoteRequest;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationElementType;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertHandler;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TrackNoteCurationUpsertHandler implements CurationUpsertHandler<UpsertTrackNoteRequest> {

    private final TrackNoteCurationService trackNoteCurationService;

    @Override
    public CurationElementType type() {
        return CurationElementType.TRACK_NOTE;
    }

    @Override
    public String identifier(UpsertTrackNoteRequest request) {
        return request.spotifyTrackId();
    }

    @Override
    public CurationUpsertResult upsert(UpsertTrackNoteRequest request) {
        var upserted = trackNoteCurationService.upsert(request) ? 1 : 0;
        return new CurationUpsertResult(type(), identifier(request), upserted);
    }
}
