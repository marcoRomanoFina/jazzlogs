package com.marcoromanofinaa.jazzlogs.ai.recommend.core.candidate;

import java.util.List;

public record TrackRecommendDecisionContext(
        String tier,
        boolean instrumental,
        boolean standout,
        String jazzLogsCaptionEssence,
        List<String> mainArtists,
        List<String> artistContext,
        List<String> vibe,
        String energy,
        String moodIntensity,
        String accessibility,
        String tempoFeel,
        String rhythmicFeel,
        String trackRole,
        String compositionType,
        String bestMoment,
        List<String> listeningContext,
        String whyItHits,
        String editorialNote,
        String recommendedIf,
        String avoidIf,
        String instrumentFocus,
        String vocalStyle,
        List<String> standoutTags,
        List<String> albumPersonnel
) {
}
