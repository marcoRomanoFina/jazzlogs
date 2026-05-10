package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.List;

public record AlbumRecommendBestMoment(
        String introduccion,
        List<AlbumRecommendBestMomentItem> momentos,
        String conclusion
) {
    public AlbumRecommendBestMoment {
        momentos = momentos == null ? List.of() : List.copyOf(momentos);
    }
}
