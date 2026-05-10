package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.List;

public record AlbumRecommendDecisionContext(
        String style,
        String vocalProfile,
        String releaseYear,
        Integer totalTracks,
        String releaseDate,
        List<String> moods,
        String tier,
        List<String> vibe,
        String energy,
        String moodIntensity,
        String accessibility,
        AlbumRecommendBestMoment bestMoment,
        List<String> listeningContext,
        String notes,
        String whyItMatters,
        String editorialNote,
        String recommendedIf,
        String avoidIf,
        String albumContext,
        String captionEssence
) {
}
