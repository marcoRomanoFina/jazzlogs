package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.util.List;

public record RecommendedItemDTO(
        String winnerName,
        String sourceType,
        String album,
        String track,
        String primaryArtist,
        List<String> secondaryArtists,
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
