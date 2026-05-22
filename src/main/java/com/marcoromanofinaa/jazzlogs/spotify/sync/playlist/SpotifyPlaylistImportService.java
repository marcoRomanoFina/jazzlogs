package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtistRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import com.marcoromanofinaa.jazzlogs.spotify.exception.SpotifyCatalogImportException;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SpotifyPlaylistImportService {

    private final SpotifyArtistRepository spotifyArtistRepository;
    private final SpotifyAlbumRepository spotifyAlbumRepository;
    private final SpotifyTrackRepository spotifyTrackRepository;

    @Transactional
    public void importPlaylistTracks(List<SpotifyPlaylistTrackDTO> tracks) {
        try {
            if (tracks == null || tracks.isEmpty()) {
                return;
            }

            var importableTracks = tracks.stream()
                    .filter(this::isImportableTrack)
                    .toList();

            if (importableTracks.isEmpty()) {
                return;
            }

            Instant now = Instant.now();
            var artistsBySpotifyId = upsertArtists(importableTracks, now);
            var albumsBySpotifyId = upsertAlbums(importableTracks, artistsBySpotifyId, now);
            upsertTracks(importableTracks, artistsBySpotifyId, albumsBySpotifyId, now);
        }
        catch (RuntimeException exception) {
            throw new SpotifyCatalogImportException("Failed to import Spotify playlist tracks into catalog", exception);
        }
    }

    private Map<String, SpotifyArtist> upsertArtists(List<SpotifyPlaylistTrackDTO> tracks, Instant now) {
        var artistDtosBySpotifyId = new LinkedHashMap<String, SpotifyArtistDTO>();
        for (SpotifyPlaylistTrackDTO track : tracks) {
            collectArtists(track.artists(), artistDtosBySpotifyId);
            collectArtists(track.album().artists(), artistDtosBySpotifyId);
        }

        var existingArtistsBySpotifyId = indexArtistsBySpotifyId(
                spotifyArtistRepository.findAllBySpotifyArtistIdIn(artistDtosBySpotifyId.keySet())
        );

        for (SpotifyArtistDTO artist : artistDtosBySpotifyId.values()) {
            var spotifyArtist = existingArtistsBySpotifyId.computeIfAbsent(
                    artist.spotifyArtistId(),
                    spotifyArtistId -> SpotifyArtist.create(
                            spotifyArtistId,
                            defaultText(artist.name()),
                            artist.spotifyUrl(),
                            now
                    )
            );
            spotifyArtist.updateMetadata(defaultText(artist.name()), artist.spotifyUrl(), now);
        }

        spotifyArtistRepository.saveAll(existingArtistsBySpotifyId.values());
        return existingArtistsBySpotifyId;
    }

    private Map<String, SpotifyAlbum> upsertAlbums(
            List<SpotifyPlaylistTrackDTO> tracks,
            Map<String, SpotifyArtist> artistsBySpotifyId,
            Instant now
    ) {
        var albumDtosBySpotifyId = new LinkedHashMap<String, SpotifyAlbumDTO>();
        for (SpotifyPlaylistTrackDTO track : tracks) {
            albumDtosBySpotifyId.put(track.album().spotifyAlbumId(), track.album());
        }

        var existingAlbumsBySpotifyId = indexAlbumsBySpotifyId(
                spotifyAlbumRepository.findAllBySpotifyAlbumIdIn(albumDtosBySpotifyId.keySet())
        );

        for (SpotifyAlbumDTO album : albumDtosBySpotifyId.values()) {
            var spotifyAlbum = existingAlbumsBySpotifyId.computeIfAbsent(
                    album.spotifyAlbumId(),
                    spotifyAlbumId -> SpotifyAlbum.create(
                            spotifyAlbumId,
                            defaultText(album.name()),
                            album.releaseDate(),
                            album.totalTracks(),
                            album.imageUrl(),
                            album.spotifyUrl(),
                            now
                    )
            );

            spotifyAlbum.updateMetadata(
                    defaultText(album.name()),
                    album.releaseDate(),
                    album.totalTracks(),
                    album.imageUrl(),
                    album.spotifyUrl(),
                    now
            );
            spotifyAlbum.replaceArtistsInSpotifyOrder(resolveArtistsInSpotifyOrder(album.artists(), artistsBySpotifyId));
        }

        spotifyAlbumRepository.saveAll(existingAlbumsBySpotifyId.values());
        return existingAlbumsBySpotifyId;
    }

    private void upsertTracks(
            List<SpotifyPlaylistTrackDTO> tracks,
            Map<String, SpotifyArtist> artistsBySpotifyId,
            Map<String, SpotifyAlbum> albumsBySpotifyId,
            Instant now
    ) {
        var trackIds = tracks.stream()
                .map(SpotifyPlaylistTrackDTO::spotifyTrackId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        var existingTracksBySpotifyId = indexTracksBySpotifyId(
                spotifyTrackRepository.findAllBySpotifyTrackIdIn(trackIds)
        );

        for (SpotifyPlaylistTrackDTO track : tracks) {
            var album = albumsBySpotifyId.get(track.album().spotifyAlbumId());
            var spotifyTrack = existingTracksBySpotifyId.computeIfAbsent(
                    track.spotifyTrackId(),
                    spotifyTrackId -> SpotifyTrack.create(
                            spotifyTrackId,
                            defaultText(track.name()),
                            album,
                            track.durationMs(),
                            track.trackNumber(),
                            track.spotifyUrl(),
                            now
                    )
            );

            spotifyTrack.updateMetadata(
                    defaultText(track.name()),
                    album,
                    track.durationMs(),
                    track.trackNumber(),
                    track.spotifyUrl(),
                    now
            );
            spotifyTrack.replaceArtistsInSpotifyOrder(resolveArtistsInSpotifyOrder(track.artists(), artistsBySpotifyId));
        }

        spotifyTrackRepository.saveAll(existingTracksBySpotifyId.values());
    }

    private void collectArtists(
            List<SpotifyArtistDTO> artists,
            Map<String, SpotifyArtistDTO> artistDtosBySpotifyId
    ) {
        if (artists == null || artists.isEmpty()) {
            return;
        }

        for (SpotifyArtistDTO artist : artists) {
            if (artist == null || isBlank(artist.spotifyArtistId())) {
                continue;
            }
            artistDtosBySpotifyId.put(artist.spotifyArtistId(), artist);
        }
    }

    private List<SpotifyArtist> resolveArtistsInSpotifyOrder(
            List<SpotifyArtistDTO> artists,
            Map<String, SpotifyArtist> artistsBySpotifyId
    ) {
        List<SpotifyArtist> resolvedArtists = new ArrayList<>();
        if (artists == null || artists.isEmpty()) {
            return resolvedArtists;
        }

        for (SpotifyArtistDTO artist : artists) {
            if (artist == null || isBlank(artist.spotifyArtistId())) {
                continue;
            }

            var spotifyArtist = artistsBySpotifyId.get(artist.spotifyArtistId());
            if (spotifyArtist != null) {
                resolvedArtists.add(spotifyArtist);
            }
        }

        return resolvedArtists;
    }

    private Map<String, SpotifyArtist> indexArtistsBySpotifyId(Collection<SpotifyArtist> artists) {
        var artistsBySpotifyId = new LinkedHashMap<String, SpotifyArtist>();
        for (SpotifyArtist artist : artists) {
            artistsBySpotifyId.put(artist.getSpotifyArtistId(), artist);
        }
        return artistsBySpotifyId;
    }

    private Map<String, SpotifyAlbum> indexAlbumsBySpotifyId(Collection<SpotifyAlbum> albums) {
        var albumsBySpotifyId = new LinkedHashMap<String, SpotifyAlbum>();
        for (SpotifyAlbum album : albums) {
            albumsBySpotifyId.put(album.getSpotifyAlbumId(), album);
        }
        return albumsBySpotifyId;
    }

    private Map<String, SpotifyTrack> indexTracksBySpotifyId(Collection<SpotifyTrack> tracks) {
        var tracksBySpotifyId = new LinkedHashMap<String, SpotifyTrack>();
        for (SpotifyTrack track : tracks) {
            tracksBySpotifyId.put(track.getSpotifyTrackId(), track);
        }
        return tracksBySpotifyId;
    }

    private boolean isImportableTrack(SpotifyPlaylistTrackDTO track) {
        return track != null
                && !isBlank(track.spotifyTrackId())
                && track.album() != null
                && !isBlank(track.album().spotifyAlbumId());
    }

    private String defaultText(String value) {
        return value == null ? "" : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
