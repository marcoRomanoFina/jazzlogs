package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto;

import java.util.List;

public record SpotifyAlbumDTO(
        String spotifyAlbumId,
        String name,
        List<SpotifyArtistDTO> artists,
        String releaseDate,
        Integer totalTracks,
        String imageUrl,
        String spotifyUrl
) {
}
