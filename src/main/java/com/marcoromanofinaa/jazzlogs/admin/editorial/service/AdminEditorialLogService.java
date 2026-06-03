package com.marcoromanofinaa.jazzlogs.admin.editorial.service;

import com.marcoromanofinaa.jazzlogs.admin.editorial.album.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto.UpsertAlbumLogRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogBestMomentItem;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogMainArtist;
import com.marcoromanofinaa.jazzlogs.admin.editorial.album.model.AlbumLogPersonnel;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.ArtistLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.dto.UpsertArtistLogRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.artist.model.ArtistLog;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.TrackLogRepository;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.dto.UpsertTrackLogRequestDTO;
import com.marcoromanofinaa.jazzlogs.admin.editorial.track.model.TrackLog;
import com.marcoromanofinaa.jazzlogs.core.outbox.editorial.EditorialLogIndexingOutboxPublisher;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminEditorialLogService {

    private final AlbumLogRepository albumLogRepository;
    private final ArtistLogRepository artistLogRepository;
    private final TrackLogRepository trackLogRepository;
    private final SpotifyTrackRepository spotifyTrackRepository;
    private final EditorialLogIndexingOutboxPublisher outboxPublisher;

    @Transactional
    public void upsertArtistLog(UUID authenticatedUserId, UpsertArtistLogRequestDTO request) {
        var artistData = request.artistData();
        var existingArtistLog = artistLogRepository.findBySpotifyArtistId(artistData.spotifyArtistId());
        var artistLog = existingArtistLog
                .orElseGet(() -> ArtistLog.create(
                        artistData.spotifyArtistId(),
                        artistData.artistName(),
                        artistData.primaryInstrument(),
                        artistData.mainStyles(),
                        artistData.soundProfile(),
                        artistData.artistContext(),
                        artistData.editorialNote(),
                        artistData.entryPointLogId(),
                        artistData.bestListeningMoments(),
                        artistData.avoidIf(),
                        defaultList(artistData.relatedArtists()),
                        artistData.whyItMatters(),
                        defaultIntegerList(artistData.appearsInLogs())
                ));
        boolean isNewArtistLog = existingArtistLog.isEmpty();

        artistLog.apply(
                artistData.spotifyArtistId(),
                artistData.artistName(),
                artistData.primaryInstrument(),
                artistData.mainStyles(),
                artistData.soundProfile(),
                artistData.artistContext(),
                artistData.editorialNote(),
                artistData.entryPointLogId(),
                artistData.bestListeningMoments(),
                artistData.avoidIf(),
                defaultList(artistData.relatedArtists()),
                artistData.whyItMatters(),
                defaultIntegerList(artistData.appearsInLogs())
        );
        if (isNewArtistLog) {
            artistLog.markIndexingPending();
        } else {
            artistLog.markIndexingStale();
        }

        var savedArtistLog = artistLogRepository.save(artistLog);
        outboxPublisher.publishArtistLogIndexingRequested(savedArtistLog, authenticatedUserId);
    }

    @Transactional
    public void upsertTrackLog(UUID authenticatedUserId, UpsertTrackLogRequestDTO request) {
        var trackData = request.trackData();
        
        var primaryArtist = trackData.primaryArtist();
        if (primaryArtist == null || primaryArtist.isBlank()) {
            primaryArtist = spotifyTrackRepository.findBySpotifyTrackId(trackData.spotifyTrackId())
                    .map(spotifyTrack -> spotifyTrack.getArtists().isEmpty() ? null : spotifyTrack.getArtists().getFirst().getName())
                    .or(() -> albumLogRepository.findBySpotifyAlbumId(trackData.spotifyAlbumId())
                            .map(album -> album.getMainArtists().isEmpty() ? null : album.getMainArtists().getFirst().name()))
                    .orElse(null);
        }

        var finalPrimaryArtist = primaryArtist;
        var existingTrackLog = trackLogRepository.findBySpotifyTrackId(trackData.spotifyTrackId());
        var trackLog = existingTrackLog
                .orElseGet(() -> TrackLog.create(
                        trackData.spotifyTrackId(),
                        trackData.spotifyAlbumId(),
                        trackData.logNumber(),
                        trackData.trackName(),
                        trackData.albumName(),
                        finalPrimaryArtist,
                        trackData.mainArtistSpotifyId(),
                        trackData.tier(),
                        trackData.vocalProfile(),
                        trackData.standout(),
                        trackData.vibe(),
                        trackData.energy(),
                        trackData.moodIntensity(),
                        trackData.accessibility(),
                        trackData.tempoFeel(),
                        trackData.rhythmFeel(),
                        trackData.albumRole(),
                        trackData.compositionType(),
                        trackData.bestMoment(),
                        trackData.listeningContext(),
                        trackData.whyItHits(),
                        trackData.editorialNote(),
                        trackData.recommendedIf(),
                        trackData.avoidIf(),
                        trackData.instrumentFocus(),
                        trackData.vocalStyle(),
                        defaultList(trackData.standoutTags())
                ));
        boolean isNewTrackLog = existingTrackLog.isEmpty();

        trackLog.apply(
                trackData.spotifyTrackId(),
                trackData.spotifyAlbumId(),
                trackData.logNumber(),
                trackData.trackName(),
                trackData.albumName(),
                finalPrimaryArtist,
                trackData.mainArtistSpotifyId(),
                trackData.tier(),
                trackData.vocalProfile(),
                trackData.standout(),
                trackData.vibe(),
                trackData.energy(),
                trackData.moodIntensity(),
                trackData.accessibility(),
                trackData.tempoFeel(),
                trackData.rhythmFeel(),
                trackData.albumRole(),
                trackData.compositionType(),
                trackData.bestMoment(),
                trackData.listeningContext(),
                trackData.whyItHits(),
                trackData.editorialNote(),
                trackData.recommendedIf(),
                trackData.avoidIf(),
                trackData.instrumentFocus(),
                trackData.vocalStyle(),
                defaultList(trackData.standoutTags())
        );
        if (isNewTrackLog) {
            trackLog.markIndexingPending();
        } else {
            trackLog.markIndexingStale();
        }

        var savedTrackLog = trackLogRepository.save(trackLog);
        outboxPublisher.publishTrackLogIndexingRequested(savedTrackLog, authenticatedUserId);
    }

    @Transactional
    public void upsertAlbumLog(UUID authenticatedUserId, UpsertAlbumLogRequestDTO request) {
        var albumData = request.albumData();
        var existingAlbumLog = albumLogRepository.findBySpotifyAlbumId(albumData.spotifyAlbumId())
                .or(() -> albumLogRepository.findByLogNumber(albumData.logNumber()))
                ;
        var albumLog = existingAlbumLog.orElseGet(() -> AlbumLog.create(
                        albumData.logNumber(),
                        albumData.albumName(),
                        mapMainArtists(albumData.mainArtists()),
                        albumData.captionEssence(),
                        albumData.postedAt(),
                        albumData.instagramPermalink(),
                        albumData.style(),
                        albumData.vocalProfile(),
                        String.valueOf(albumData.releaseYear()),
                        arrayToList(albumData.moods()),
                        albumData.tier(),
                        arrayToList(albumData.vibe()),
                        albumData.energy(),
                        albumData.moodIntensity(),
                        albumData.accessibility(),
                        mapBestMoment(albumData.bestMoment()),
                        arrayToList(albumData.listeningContext()),
                        albumData.whyItMatters(),
                        albumData.editorialNote(),
                        albumData.recommendedIf(),
                        albumData.avoidIf(),
                        albumData.albumContext(),
                        mapPersonnel(albumData.personnel()),
                        albumData.spotifyAlbumId()
                ));
        boolean isNewAlbumLog = existingAlbumLog.isEmpty();

        albumLog.apply(
                albumData.logNumber(),
                albumData.albumName(),
                mapMainArtists(albumData.mainArtists()),
                albumData.captionEssence(),
                albumData.postedAt(),
                albumData.instagramPermalink(),
                albumData.style(),
                albumData.vocalProfile(),
                String.valueOf(albumData.releaseYear()),
                arrayToList(albumData.moods()),
                albumData.tier(),
                arrayToList(albumData.vibe()),
                albumData.energy(),
                albumData.moodIntensity(),
                albumData.accessibility(),
                mapBestMoment(albumData.bestMoment()),
                arrayToList(albumData.listeningContext()),
                albumData.whyItMatters(),
                albumData.editorialNote(),
                albumData.recommendedIf(),
                albumData.avoidIf(),
                albumData.albumContext(),
                mapPersonnel(albumData.personnel()),
                albumData.spotifyAlbumId()
        );
        if (isNewAlbumLog) {
            albumLog.markIndexingPending();
        } else {
            albumLog.markIndexingStale();
        }

        var savedAlbumLog = albumLogRepository.save(albumLog);
        outboxPublisher.publishAlbumLogIndexingRequested(savedAlbumLog, authenticatedUserId);
    }

    private List<AlbumLogMainArtist> mapMainArtists(
            List<com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto.AlbumLogMainArtist> mainArtists
    ) {
        return mainArtists.stream()
                .map(artist -> new AlbumLogMainArtist(artist.spotifyArtistId(), artist.name()))
                .toList();
    }

    private AlbumLogBestMoment mapBestMoment(
            com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto.AlbumLogBestMoment bestMoment
    ) {
        if (bestMoment == null) {
            return null;
        }

        var moments = bestMoment.momentos() == null ? List.<AlbumLogBestMomentItem>of() : bestMoment.momentos().stream()
                .map(item -> new AlbumLogBestMomentItem(item.momento(), item.descripcion()))
                .toList();

        return new AlbumLogBestMoment(
                bestMoment.introduccion(),
                moments,
                bestMoment.conclusion()
        );
    }

    private List<AlbumLogPersonnel> mapPersonnel(
            List<com.marcoromanofinaa.jazzlogs.admin.editorial.album.dto.AlbumLogPersonnel> personnel
    ) {
        if (personnel == null) {
            return List.of();
        }

        return personnel.stream()
                .map(member -> new AlbumLogPersonnel(member.spotifyArtistId(), member.name(), member.role()))
                .toList();
    }

    private List<String> arrayToList(String[] values) {
        return values == null ? List.of() : Arrays.asList(values);
    }

    private List<String> defaultList(List<String> values) {
        return values == null ? List.of() : values;
    }

    private List<Integer> defaultIntegerList(List<Integer> values) {
        return values == null ? List.of() : values;
    }
}
