package com.marcoromanofinaa.jazzlogs.spotify.catalog;

public record SpotifyAlbumSyncData(
        String sourcePlaylistId,
        String name,
        String spotifyUrl,
        String coverImageUrl,
        String albumType,
        Integer totalTracks,
        String releaseDate,
        String releaseDatePrecision
) {
}
