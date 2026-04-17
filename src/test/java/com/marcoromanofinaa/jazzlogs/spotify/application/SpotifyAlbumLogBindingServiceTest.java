package com.marcoromanofinaa.jazzlogs.spotify.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyAlbumRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotifyAlbumLogBindingServiceTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Mock
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Mock
    private SpotifyProperties spotifyProperties;

    @InjectMocks
    private SpotifyAlbumLogBindingService service;

    @Test
    void bindsUnlinkedLogsUsingSpotifyAlbumSeedId() {
        var album = SpotifyAlbum.builder()
                .spotifyAlbumId("album-1")
                .sourcePlaylistId("playlist-1")
                .name("Bound Album")
                .build();
        var log = AlbumLog.create(
                1,
                "Bound Album",
                "Bound Artist",
                "Caption",
                LocalDate.of(2026, 4, 17),
                "https://www.instagram.com/p/BOUND123/",
                "Hard Bop",
                new String[]{"warm"},
                "Notes",
                "album-1"
        );

        when(spotifyProperties.playlistId()).thenReturn("playlist-1");
        when(spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc("playlist-1"))
                .thenReturn(List.of(album));
        when(albumLogRepository.findAllBySpotifyAlbumIsNullOrderByLogNumberAsc())
                .thenReturn(List.of(log));

        var boundCount = service.bindConfiguredPlaylistAlbumsToLogs();

        assertThat(boundCount).isEqualTo(1);
        assertThat(log.getSpotifyAlbum()).isSameAs(album);
        verify(albumLogRepository).saveAll(any());
    }

    @Test
    void leavesLogsUntouchedWhenSeedIdDoesNotResolve() {
        var log = AlbumLog.create(
                1,
                "Unbound Album",
                "Unbound Artist",
                "Caption",
                LocalDate.of(2026, 4, 17),
                "https://www.instagram.com/p/UNBOUND123/",
                "Hard Bop",
                new String[]{"warm"},
                "Notes",
                "missing-album"
        );

        when(spotifyProperties.playlistId()).thenReturn("playlist-1");
        when(spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc("playlist-1"))
                .thenReturn(List.of());
        when(albumLogRepository.findAllBySpotifyAlbumIsNullOrderByLogNumberAsc())
                .thenReturn(List.of(log));

        var boundCount = service.bindConfiguredPlaylistAlbumsToLogs();

        assertThat(boundCount).isZero();
        assertThat(log.getSpotifyAlbum()).isNull();
        verify(albumLogRepository, never()).saveAll(any());
    }
}
