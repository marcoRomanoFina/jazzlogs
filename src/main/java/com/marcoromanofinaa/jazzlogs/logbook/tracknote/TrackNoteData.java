package com.marcoromanofinaa.jazzlogs.logbook.tracknote;

public record TrackNoteData(
        String spotifyTrackId,
        String spotifyAlbumId,
        Integer logNumber,
        String track,
        String album,
        String artistId,
        String tier,
        boolean instrumental,
        boolean standout,
        String[] vibe,
        String energy,
        String moodIntensity,
        String accessibility,
        String tempoFeel,
        String rhythmicFeel,
        String trackRole,
        String compositionType,
        String bestMoment,
        String[] listeningContext,
        String whyItHits,
        String editorialNote,
        String recommendedIf,
        String avoidIf,
        String instrumentFocus,
        String vocalStyle,
        String[] standoutTags
) {
}
