package com.marcoromanofinaa.jazzlogs.spotify.catalog;

import java.time.Instant;
import java.util.Set;

public record SpotifyTrackSyncData(
        String sourcePlaylistId,
        SpotifyAlbum album,
        SpotifyArtist mainArtist,
        Set<SpotifyArtist> secondaryArtists,
        String name,
        String spotifyUrl,
        Integer durationMs,
        Integer discNumber,
        Integer trackNumber,
        Instant addedToPlaylistAt
) {
}
