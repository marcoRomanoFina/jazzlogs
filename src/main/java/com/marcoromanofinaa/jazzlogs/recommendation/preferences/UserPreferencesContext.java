package com.marcoromanofinaa.jazzlogs.recommendation.preferences;

import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyTopUserArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.dto.SpotifyUserTopTrackDTO;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.dto.UserJazzPreferencesDto;
import java.util.List;

public record UserPreferencesContext(
        UserJazzPreferencesDto jazzPreferences,
        List<SpotifyTopUserArtistDTO> topArtists,
        List<SpotifyUserTopTrackDTO> topTracks
) {
}
