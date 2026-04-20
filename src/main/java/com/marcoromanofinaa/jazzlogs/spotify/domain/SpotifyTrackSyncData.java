package com.marcoromanofinaa.jazzlogs.spotify.domain;

import java.time.OffsetDateTime;
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
        OffsetDateTime addedToPlaylistAt
) {
}
