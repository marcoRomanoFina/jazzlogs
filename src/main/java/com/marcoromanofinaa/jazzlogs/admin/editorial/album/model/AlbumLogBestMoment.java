package com.marcoromanofinaa.jazzlogs.admin.editorial.album.model;

import java.util.List;

public record AlbumLogBestMoment(
        String introduccion,
        List<AlbumLogBestMomentItem> momentos,
        String conclusion
) {
}
