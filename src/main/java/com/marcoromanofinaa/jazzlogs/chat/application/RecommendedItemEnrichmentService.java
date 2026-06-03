package com.marcoromanofinaa.jazzlogs.chat.application;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.TrackLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.RecommendedItemDTO;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RecommendedItemEnrichmentService {

    private final AlbumLogRepository albumLogRepository;
    private final TrackLogRepository trackLogRepository;
    private final SpotifyAlbumRepository spotifyAlbumRepository;
    private final SpotifyTrackRepository spotifyTrackRepository;

    public List<RecommendedItemDTO> enrich(List<String> winners) {
        if (winners == null || winners.isEmpty()) {
            return List.of();
        }
        return winners.stream()
                .map(this::enrichWinner)
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<RecommendedItemDTO> enrichWinner(String winner) {
        if (winner == null || winner.isBlank()) {
            return Optional.empty();
        }

        var albumLog = albumLogRepository.findFirstByAlbumNameIgnoreCase(winner.trim());
        if (albumLog.isPresent()) {
            return Optional.of(fromAlbum(winner, albumLog.get(), spotifyAlbum(albumLog.get()).orElse(null)));
        }

        var trackLog = trackLogRepository.findFirstByTrackNameIgnoreCase(winner.trim());
        if (trackLog.isPresent()) {
            var spotifyTrack = spotifyTrackRepository.findBySpotifyTrackId(trackLog.get().getSpotifyTrackId());
            var album = albumFor(trackLog.get(), spotifyTrack.orElse(null));
            return Optional.of(fromTrack(winner, trackLog.get(), album, spotifyTrack.orElse(null)));
        }

        return Optional.empty();
    }

    private RecommendedItemDTO fromAlbum(String winnerName, AlbumLog albumLog, SpotifyAlbum spotifyAlbum) {
        return new RecommendedItemDTO(
                winnerName,
                "ALBUM_LOG",
                albumLog.getAlbumName(),
                null,
                firstArtistName(albumLog).orElse(null),
                secondaryArtistNames(albumLog),
                albumLog.getLogNumber(),
                albumLog.getTier(),
                albumLog.getReleaseYear(),
                albumLog.getSpotifyAlbumId(),
                null,
                spotifyAlbum == null ? null : spotifyAlbum.getSpotifyUrl(),
                spotifyAlbum == null ? null : spotifyAlbum.getImageUrl(),
                albumLog.getInstagramPermalink()
        );
    }

    private RecommendedItemDTO fromTrack(
            String winnerName,
            TrackLog trackLog,
            AlbumLog albumLog,
            SpotifyTrack spotifyTrack
    ) {
        var spotifyAlbum = spotifyTrack == null ? null : spotifyTrack.getAlbum();
        return new RecommendedItemDTO(
                winnerName,
                "TRACK_LOG",
                trackLog.getAlbumName(),
                trackLog.getTrackName(),
                primaryArtistForTrack(trackLog, albumLog, spotifyTrack),
                secondaryArtistsForTrack(albumLog, spotifyTrack),
                trackLog.getLogNumber(),
                trackLog.getTier(),
                albumLog == null ? null : albumLog.getReleaseYear(),
                trackLog.getSpotifyAlbumId(),
                trackLog.getSpotifyTrackId(),
                spotifyTrack == null ? null : spotifyTrack.getSpotifyUrl(),
                spotifyAlbum == null ? null : spotifyAlbum.getImageUrl(),
                albumLog == null ? null : albumLog.getInstagramPermalink()
        );
    }

    private Optional<SpotifyAlbum> spotifyAlbum(AlbumLog albumLog) {
        if (albumLog.getSpotifyAlbumId() == null || albumLog.getSpotifyAlbumId().isBlank()) {
            return Optional.empty();
        }
        return spotifyAlbumRepository.findBySpotifyAlbumId(albumLog.getSpotifyAlbumId());
    }

    private AlbumLog albumFor(TrackLog trackLog, SpotifyTrack spotifyTrack) {
        if (trackLog.getSpotifyAlbumId() != null && !trackLog.getSpotifyAlbumId().isBlank()) {
            var bySpotifyId = albumLogRepository.findBySpotifyAlbumId(trackLog.getSpotifyAlbumId());
            if (bySpotifyId.isPresent()) {
                return bySpotifyId.get();
            }
        }
        if (trackLog.getAlbumName() != null && !trackLog.getAlbumName().isBlank()) {
            var byName = albumLogRepository.findFirstByAlbumNameIgnoreCase(trackLog.getAlbumName());
            if (byName.isPresent()) {
                return byName.get();
            }
        }
        if (spotifyTrack != null && spotifyTrack.getAlbum() != null) {
            return albumLogRepository.findBySpotifyAlbumId(spotifyTrack.getAlbum().getSpotifyAlbumId()).orElse(null);
        }
        return null;
    }

    private String primaryArtistForTrack(TrackLog trackLog, AlbumLog albumLog, SpotifyTrack spotifyTrack) {
        if (spotifyTrack != null && spotifyTrack.getArtists() != null && !spotifyTrack.getArtists().isEmpty()) {
            var artist = spotifyTrack.getArtists().getFirst();
            if (artist != null && artist.getName() != null && !artist.getName().isBlank()) {
                return artist.getName();
            }
        }
        return firstArtistName(albumLog).orElse(null);
    }

    private List<String> secondaryArtistsForTrack(AlbumLog albumLog, SpotifyTrack spotifyTrack) {
        if (spotifyTrack != null && spotifyTrack.getArtists() != null && spotifyTrack.getArtists().size() > 1) {
            return spotifyTrack.getArtists().stream()
                    .skip(1)
                    .map(artist -> artist.getName())
                    .filter(name -> name != null && !name.isBlank())
                    .toList();
        }
        return secondaryArtistNames(albumLog);
    }

    private Optional<String> firstArtistName(AlbumLog albumLog) {
        if (albumLog == null || albumLog.getMainArtists() == null || albumLog.getMainArtists().isEmpty()) {
            return Optional.empty();
        }
        return albumLog.getMainArtists().stream()
                .map(artist -> artist.name())
                .filter(name -> name != null && !name.isBlank())
                .findFirst();
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
