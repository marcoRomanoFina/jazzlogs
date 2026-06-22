package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.util.List;

public record RecommendedItemDTO(
        String winnerNodeId,
        BasicRecommendationTarget recommendationType,
        String album,
        String track,
        List<String> mainArtists,
        Integer logNumber,
        String tier,
        String releaseYear,
        String spotifyAlbumId,
        String spotifyTrackId,
        String spotifyUrl,
        String spotifyImageUrl,
        String instagramPermalink
) {
}
