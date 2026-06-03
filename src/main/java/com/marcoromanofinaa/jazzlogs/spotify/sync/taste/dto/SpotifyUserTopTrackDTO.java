package com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto;

import java.util.List;

public record SpotifyUserTopTrackDTO(
        String name,
        List<String> artistNames,
        String albumName
) {
}
