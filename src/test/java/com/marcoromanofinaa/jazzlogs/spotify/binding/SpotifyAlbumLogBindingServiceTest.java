package com.marcoromanofinaa.jazzlogs.spotify.binding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogData;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyProperties;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SpotifyAlbumLogBindingServiceTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Mock
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Test
    void bindsUnlinkedLogsUsingSpotifyAlbumSeedId() {
        var service = service();
        var album = SpotifyAlbum.builder()
                .spotifyAlbumId("album-1")
                .sourcePlaylistId("playlist-1")
                .name("Bound Album")
                .build();
        var log = AlbumLog.create(new AlbumLogData(
                1,
                "Bound Album",
                "Bound Artist",
                "Caption",
                LocalDate.of(2026, 4, 17),
                "https://www.instagram.com/p/BOUND123/",
                "Hard Bop",
                null,
                new String[]{"warm"},
                null,
                new String[]{},
                null,
                null,
                null,
                null,
                new String[]{},
                "Notes",
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                "album-1"
        ));

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
        var service = service();
        var log = AlbumLog.create(new AlbumLogData(
                1,
                "Unbound Album",
                "Unbound Artist",
                "Caption",
                LocalDate.of(2026, 4, 17),
                "https://www.instagram.com/p/UNBOUND123/",
                "Hard Bop",
                null,
                new String[]{"warm"},
                null,
                new String[]{},
                null,
                null,
                null,
                null,
                new String[]{},
                "Notes",
                null,
                null,
                null,
                null,
                null,
                java.util.List.of(),
                "missing-album"
        ));

        when(spotifyAlbumRepository.findAllBySourcePlaylistIdOrderByNameAsc("playlist-1"))
                .thenReturn(List.of());
        when(albumLogRepository.findAllBySpotifyAlbumIsNullOrderByLogNumberAsc())
                .thenReturn(List.of(log));

        var boundCount = service.bindConfiguredPlaylistAlbumsToLogs();

        assertThat(boundCount).isZero();
        assertThat(log.getSpotifyAlbum()).isNull();
        verify(albumLogRepository, never()).saveAll(any());
    }

    private SpotifyAlbumLogBindingService service() {
        return new SpotifyAlbumLogBindingService(
                new SpotifyProperties(null, null, null, "playlist-1", "AR", true, null, null, false),
                albumLogRepository,
                spotifyAlbumRepository
        );
    }
}
