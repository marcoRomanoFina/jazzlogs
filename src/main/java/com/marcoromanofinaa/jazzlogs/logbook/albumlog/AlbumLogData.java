package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import java.time.LocalDate;
import java.util.List;

public record AlbumLogData(
        Integer logNumber,
        String album,
        String artist,
        String caption,
        LocalDate postedAt,
        String instagramPermalink,
        String style,
        String releaseYear,
        String[] moods,
        String tier,
        String[] vibe,
        String energy,
        String moodIntensity,
        String accessibility,
        String bestMoment,
        String[] listeningContext,
        String notes,
        String whyItMatters,
        String editorialNote,
        String recommendedIf,
        String avoidIf,
        String albumContext,
        List<AlbumLogPersonnel> personnel,
        String spotifyAlbumSeedId
) {
}
