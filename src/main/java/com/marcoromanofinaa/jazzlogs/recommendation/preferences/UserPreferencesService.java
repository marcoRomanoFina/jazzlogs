package com.marcoromanofinaa.jazzlogs.recommendation.preferences;

import com.marcoromanofinaa.jazzlogs.spotify.sync.taste.SpotifyTasteSnapshotRepository;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.mapper.UserJazzPreferencesMapper;
import com.marcoromanofinaa.jazzlogs.user.jazzpreferences.service.UserGraphPreferencesService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserPreferencesService {

    private final UserGraphPreferencesService userGraphPreferencesService;
    private final UserJazzPreferencesMapper userJazzPreferencesMapper;
    private final SpotifyTasteSnapshotRepository spotifyTasteSnapshotRepository;

    public UserPreferencesContext getPreferencesContext(UUID userId) {
        var jazzPreferences = userGraphPreferencesService.findByUserId(userId)
                .flatMap(userJazzPreferencesMapper::toDTO)
                .orElse(null);

        var snapshot = spotifyTasteSnapshotRepository.findTopByUserIdOrderByGeneratedAtDesc(userId);
        var topArtists = snapshot
                .map(value -> safeList(value.getTopArtists()))
                .orElseGet(List::of);
        var topTracks = snapshot
                .map(value -> safeList(value.getTopTracks()))
                .orElseGet(List::of);

        return new UserPreferencesContext(
                jazzPreferences,
                topArtists,
                topTracks
        );
    }

    private <T> List<T> safeList(List<T> values) {
        return values == null ? List.of() : values;
    }
}
