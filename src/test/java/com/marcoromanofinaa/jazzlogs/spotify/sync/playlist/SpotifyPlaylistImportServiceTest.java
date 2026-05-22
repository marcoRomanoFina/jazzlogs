package com.marcoromanofinaa.jazzlogs.spotify.sync.playlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtist;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyArtistRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrack;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyTrackRepository;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyAlbumDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyArtistDTO;
import com.marcoromanofinaa.jazzlogs.spotify.sync.playlist.dto.SpotifyPlaylistTrackDTO;
import java.util.List;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotifyPlaylistImportServiceTest {

    @Mock
    private SpotifyArtistRepository spotifyArtistRepository;

    @Mock
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Mock
    private SpotifyTrackRepository spotifyTrackRepository;

    @Test
    void importPlaylistTracksCreatesCatalogEntriesAndPreservesArtistOrder() {
        var service = new SpotifyPlaylistImportService(
                spotifyArtistRepository,
                spotifyAlbumRepository,
                spotifyTrackRepository
        );
        var artistOne = new SpotifyArtistDTO("artist-1", "Miles Davis", "https://spotify/artist-1");
        var artistTwo = new SpotifyArtistDTO("artist-2", "John Coltrane", "https://spotify/artist-2");
        var album = new SpotifyAlbumDTO(
                "album-1",
                "Kind of Blue",
                List.of(artistTwo, artistOne),
                "1959-08-17",
                5,
                "https://img/kind-of-blue",
                "https://spotify/album-1"
        );
        var track = new SpotifyPlaylistTrackDTO(
                "track-1",
                "So What",
                List.of(artistOne, artistTwo),
                album,
                545000,
                1,
                "https://spotify/track-1"
        );

        when(spotifyArtistRepository.findAllBySpotifyArtistIdIn(anyCollection())).thenReturn(List.of());
        when(spotifyArtistRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<SpotifyArtist>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(spotifyAlbumRepository.findAllBySpotifyAlbumIdIn(anyCollection())).thenReturn(List.of());
        when(spotifyAlbumRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<SpotifyAlbum>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(spotifyTrackRepository.findAllBySpotifyTrackIdIn(anyCollection())).thenReturn(List.of());
        when(spotifyTrackRepository.saveAll(org.mockito.ArgumentMatchers.<Iterable<SpotifyTrack>>any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.importPlaylistTracks(List.of(track));

        verify(spotifyAlbumRepository).saveAll(argThat(iterable -> {
            var savedAlbum = StreamSupport.stream(iterable.spliterator(), false)
                    .findFirst()
                    .orElseThrow();

            assertThat(savedAlbum.getSpotifyAlbumId()).isEqualTo("album-1");
            assertThat(savedAlbum.getArtists())
                    .extracting(SpotifyArtist::getSpotifyArtistId)
                    .containsExactly("artist-2", "artist-1");
            return true;
        }));
        verify(spotifyTrackRepository).saveAll(argThat(iterable -> {
            var savedTrack = StreamSupport.stream(iterable.spliterator(), false)
                    .findFirst()
                    .orElseThrow();

            assertThat(savedTrack.getSpotifyTrackId()).isEqualTo("track-1");
            assertThat(savedTrack.getArtists())
                    .extracting(SpotifyArtist::getSpotifyArtistId)
                    .containsExactly("artist-1", "artist-2");
            assertThat(savedTrack.getAlbum().getSpotifyAlbumId()).isEqualTo("album-1");
            return true;
        }));
    }
}
