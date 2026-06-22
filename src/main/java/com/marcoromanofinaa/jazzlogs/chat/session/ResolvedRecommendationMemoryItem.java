package com.marcoromanofinaa.jazzlogs.chat.session;

import java.util.List;

public record ResolvedRecommendationMemoryItem(
        String album,
        String track,
        String primaryArtist,
        String releaseYear,
        String style,
        String vocalProfile,
        List<String> moods,
        String energy,
        String accessibility,
        String tempoFeel,
        String instrumentFocus
) {
}
