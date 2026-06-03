package com.marcoromanofinaa.jazzlogs.chat.session;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.TrackLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import com.marcoromanofinaa.jazzlogs.recommendation.basic.BasicRecommendationTarget;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendedItemMetadataLookupService {

    private final AlbumLogRepository albumLogRepository;
    private final TrackLogRepository trackLogRepository;

    public Optional<ChatRecommendationMemory.RecommendedItemMetadata> findByWinner(
            BasicRecommendationTarget recommendationType,
            String winner
    ) {
        if (winner == null || winner.isBlank()) {
            return Optional.empty();
        }

        if (recommendationType == BasicRecommendationTarget.ALBUM) {
            return albumLogRepository.findFirstByAlbumNameIgnoreCase(winner.trim())
                    .map(this::fromAlbum);
        }

        return trackLogRepository.findFirstByTrackNameIgnoreCase(winner.trim())
                .map(trackLog -> fromTrack(trackLog, albumMatchFor(trackLog)));
    }

    private AlbumLog albumMatchFor(TrackLog trackLog) {
        if (trackLog.getSpotifyAlbumId() != null && !trackLog.getSpotifyAlbumId().isBlank()) {
            return albumLogRepository.findBySpotifyAlbumId(trackLog.getSpotifyAlbumId()).orElse(null);
        }
        if (trackLog.getAlbumName() != null && !trackLog.getAlbumName().isBlank()) {
            return albumLogRepository.findFirstByAlbumNameIgnoreCase(trackLog.getAlbumName()).orElse(null);
        }
        return null;
    }

    private ChatRecommendationMemory.RecommendedItemMetadata fromAlbum(AlbumLog albumLog) {
        return new ChatRecommendationMemory.RecommendedItemMetadata(
                "ALBUM_LOG",
                albumLog.getId().toString(),
                albumLog.getAlbumName(),
                null,
                firstArtistName(albumLog),
                firstArtistName(albumLog),
                secondaryArtistNames(albumLog),
                albumLog.getSpotifyAlbumId(),
                null,
                albumLog.getTier(),
                albumLog.getReleaseYear(),
                albumLog.getStyle(),
                albumLog.getVocalProfile(),
                safeList(albumLog.getMoods()),
                safeList(albumLog.getVibe()),
                albumLog.getEnergy(),
                albumLog.getAccessibility(),
                null,
                null,
                null,
                null
        );
    }

    private ChatRecommendationMemory.RecommendedItemMetadata fromTrack(TrackLog trackLog, AlbumLog albumLog) {
        return new ChatRecommendationMemory.RecommendedItemMetadata(
                "TRACK_LOG",
                trackLog.getId().toString(),
                trackLog.getAlbumName(),
                trackLog.getTrackName(),
                firstArtistName(albumLog),
                firstArtistName(albumLog),
                secondaryArtistNames(albumLog),
                trackLog.getSpotifyAlbumId(),
                trackLog.getSpotifyTrackId(),
                trackLog.getTier(),
                albumLog == null ? null : albumLog.getReleaseYear(),
                albumLog == null ? null : albumLog.getStyle(),
                trackLog.getVocalProfile(),
                List.of(),
                safeList(trackLog.getVibe()),
                trackLog.getEnergy(),
                trackLog.getAccessibility(),
                trackLog.getTempoFeel(),
                trackLog.getInstrumentFocus(),
                null,
                null
        );
    }

    private List<String> safeList(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }

    private String firstArtistName(AlbumLog albumLog) {
        if (albumLog == null || albumLog.getMainArtists() == null || albumLog.getMainArtists().isEmpty()) {
            return null;
        }
        return albumLog.getMainArtists().stream()
                .map(artist -> artist.name())
                .filter(name -> name != null && !name.isBlank())
                .findFirst()
                .orElse(null);
    }

    private List<String> secondaryArtistNames(AlbumLog albumLog) {
        if (albumLog == null || albumLog.getMainArtists() == null || albumLog.getMainArtists().size() <= 1) {
            return List.of();
        }
        return albumLog.getMainArtists().stream()
                .skip(1)
                .map(artist -> artist.name())
                .filter(name -> name != null && !name.isBlank())
                .toList();
    }
}
