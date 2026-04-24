package com.marcoromanofinaa.jazzlogs.curation.albumlog;

import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertAlbumLogRequest;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationElementType;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertHandler;
import com.marcoromanofinaa.jazzlogs.curation.core.CurationUpsertResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AlbumLogCurationUpsertHandler implements CurationUpsertHandler<UpsertAlbumLogRequest> {

    private final AlbumLogCurationService albumLogCurationService;

    @Override
    public CurationElementType type() {
        return CurationElementType.ALBUM_LOG;
    }

    @Override
    public String identifier(UpsertAlbumLogRequest request) {
        return String.valueOf(request.logNumber());
    }

    @Override
    public CurationUpsertResult upsert(UpsertAlbumLogRequest request) {
        var upserted = albumLogCurationService.upsert(request) ? 1 : 0;
        return new CurationUpsertResult(type(), identifier(request), upserted);
    }
}
