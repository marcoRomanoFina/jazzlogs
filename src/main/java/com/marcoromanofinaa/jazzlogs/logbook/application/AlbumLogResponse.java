package com.marcoromanofinaa.jazzlogs.logbook.application;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLogPersonnel;
import java.time.LocalDate;
import java.util.List;

public record AlbumLogResponse(
        Integer logNumber,
        String album,
        String artist,
        String spotifyAlbumSeedId,
        String spotifyAlbumId,
        String caption,
        LocalDate postedAt,
        String instagramPermalink,
        String style,
        String releaseYear,
        List<String> moods,
        String tier,
        List<String> vibe,
        String energy,
        String moodIntensity,
        String accessibility,
        String bestMoment,
        List<String> listeningContext,
        String notes,
        String whyItMatters,
        String editorialNote,
        String recommendedIf,
        String avoidIf,
        String albumContext,
        List<AlbumLogPersonnel> personnel
) {

    public static AlbumLogResponse from(AlbumLog albumLog) {
        return new AlbumLogResponse(
                albumLog.getLogNumber(),
                albumLog.getAlbum(),
                albumLog.getArtist(),
                albumLog.getSpotifyAlbumSeedId(),
                albumLog.getSpotifyAlbum() != null ? albumLog.getSpotifyAlbum().getSpotifyAlbumId() : null,
                albumLog.getCaption(),
                albumLog.getPostedAt(),
                albumLog.getInstagramPermalink(),
                albumLog.getStyle(),
                albumLog.getReleaseYear(),
                List.of(albumLog.getMoods()),
                albumLog.getTier(),
                List.of(albumLog.getVibe()),
                albumLog.getEnergy(),
                albumLog.getMoodIntensity(),
                albumLog.getAccessibility(),
                albumLog.getBestMoment(),
                List.of(albumLog.getListeningContext()),
                albumLog.getNotes(),
                albumLog.getWhyItMatters(),
                albumLog.getEditorialNote(),
                albumLog.getRecommendedIf(),
                albumLog.getAvoidIf(),
                albumLog.getAlbumContext(),
                albumLog.getPersonnel()
        );
    }
}
