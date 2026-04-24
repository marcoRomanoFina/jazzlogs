package com.marcoromanofinaa.jazzlogs.spotify.catalog;

public record SpotifyArtistSyncData(
        String name,
        String spotifyUrl,
        String href,
        String uri,
        String type
) {
}
