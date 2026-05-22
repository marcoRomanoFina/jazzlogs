package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto;

import java.util.List;

public record SpotifyPlaylistTrackDTO(
        String spotifyTrackId,
        String name,
        List<SpotifyArtistDTO> artists,
        SpotifyAlbumDTO album,
        Integer durationMs,
        Integer trackNumber,
        String spotifyUrl
) {
}
