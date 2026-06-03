package com.marcoromanofinaa.jazzlogs.recommendation.basic;

import java.util.List;

public record RecommendationCandidate(
        BasicRecommendationTarget recommendationType,
        String sourceType,
        String sourceId,
        String title,
        String album,
        String track,
        String primaryArtist,
        List<String> secondaryArtists,
        String tier,
        String style,
        String vocalProfile,
        List<String> moods,
        List<String> vibe,
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
