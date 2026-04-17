package com.marcoromanofinaa.jazzlogs.logbook.application;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
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
        List<String> moods,
        String notes
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
                List.of(albumLog.getMoods()),
                albumLog.getNotes()
        );
    }
}
