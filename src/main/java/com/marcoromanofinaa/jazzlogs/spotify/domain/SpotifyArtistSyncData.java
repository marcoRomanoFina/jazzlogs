package com.marcoromanofinaa.jazzlogs.spotify.domain;

public record SpotifyArtistSyncData(
        String name,
        String spotifyUrl,
        String href,
        String uri,
        String type
) {
}
