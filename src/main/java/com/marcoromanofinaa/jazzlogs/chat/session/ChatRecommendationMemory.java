package com.marcoromanofinaa.jazzlogs.chat.session;

import java.util.List;

public record ChatRecommendationMemory(
        LastRecommendedItem lastRecommendedItem,
        List<OrderedRecommendedItem> orderedRecommendedItems,
        String sessionSummary
) {

    public record LastRecommendedItem(
            String recommendationType,
            String assistantResponse,
            List<String> winners,
            List<RecommendedItemMetadata> items
    ) {
    }

    public record OrderedRecommendedItem(
            Integer order,
            String recommendationType,
            String winnerName,
            RecommendedItemMetadata item
    ) {
    }

    public record RecommendedItemMetadata(
            String sourceType,
            String sourceId,
            String album,
            String track,
            String artistName,
            String primaryArtist,
            List<String> secondaryArtists,
            String spotifyAlbumId,
            String spotifyTrackId,
            String tier,
            String releaseYear,
            String style,
            String vocalProfile,
            List<String> moods,
            List<String> vibe,
            String energy,
            String accessibility,
            String tempoFeel,
            String instrumentFocus,
            String primaryInstrument,
            String importance
    ) {
    }
}
