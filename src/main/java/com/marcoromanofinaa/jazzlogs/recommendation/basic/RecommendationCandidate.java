package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import java.util.List;

public record RecommendationCandidate(
        String nodeId,
        BasicRecommendationTarget recommendationType,
        Integer logNumber,
        String spotifyAlbumId,
        String spotifyTrackId,
        String title,
        String album,
        String track,
        String primaryArtist,
        List<String> secondaryArtists,
        String tier,
        String style,
        String vocalProfile,
        List<String> moods,
        String energy,
        String accessibility,
        String tempoFeel,
        String instrumentFocus,
        List<String> listeningContext,
        String standout,
        String albumRole,
        String compositionType,
        String captionEssence,
        String editorialNote,
        String editorialText
) {
}
