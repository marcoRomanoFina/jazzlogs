package com.marcoromanofinaa.jazzlogs.curation.albumlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marcoromanofinaa.jazzlogs.ai.semantic.indexing.event.SemanticIndexingRequestPublisher;
import com.marcoromanofinaa.jazzlogs.curation.admin.UpsertAlbumLogRequest;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMoment;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogBestMomentItem;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogMainArtist;
import com.marcoromanofinaa.jazzlogs.logbook.albumlog.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.catalog.SpotifyAlbumRepository;
import com.marcoromanofinaa.jazzlogs.spotify.core.SpotifyException;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AlbumLogCurationServiceTest {

    @Mock
    private AlbumLogRepository albumLogRepository;

    @Mock
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Mock
    private SemanticIndexingRequestPublisher indexingRequestPublisher;

    @Test
    void linksSpotifyAlbumDirectlyDuringCreate() {
        var request = request("spotify-album-1");
        var spotifyAlbum = SpotifyAlbum.builder()
                .spotifyAlbumId("spotify-album-1")
                .name("Spunky")
                .build();
        var service = service();

        when(spotifyAlbumRepository.findById("spotify-album-1")).thenReturn(Optional.of(spotifyAlbum));
        when(albumLogRepository.findByLogNumber(1)).thenReturn(Optional.empty());

        service.upsert(request);

        var albumCaptor = ArgumentCaptor.forClass(AlbumLog.class);
        verify(albumLogRepository).save(albumCaptor.capture());
        assertThat(albumCaptor.getValue().getSpotifyAlbum()).isSameAs(spotifyAlbum);
        verify(indexingRequestPublisher).requestAlbumLogReindex(1);
    }

    @Test
    void failsFastWhenReferencedSpotifyAlbumDoesNotExist() {
        var request = request("missing-album");
        var service = service();

        when(spotifyAlbumRepository.findById("missing-album")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.upsert(request))
                .isInstanceOf(SpotifyException.class)
                .hasMessageContaining("missing-album");

        verify(albumLogRepository, never()).save(any());
        verify(indexingRequestPublisher, never()).requestAlbumLogReindex(any());
    }

    private AlbumLogCurationService service() {
        return new AlbumLogCurationService(albumLogRepository, spotifyAlbumRepository, indexingRequestPublisher);
    }

    private UpsertAlbumLogRequest request(String spotifyAlbumId) {
        return new UpsertAlbumLogRequest(
                "Spunky",
                List.of(new AlbumLogMainArtist("artist-1", "Monty Alexander")),
                "Original caption",
                LocalDate.of(2026, 2, 2),
                "https://www.instagram.com/p/DUReEzNEfyX/",
                "Hard Bop / Soul Jazz",
                "instrumental",
                "1965",
                1,
                List.of("energético", "groovy", "cálido"),
                "esencial",
                List.of("cálido", "luminoso", "festivo"),
                "alta",
                "media",
                "fácil",
                new AlbumLogBestMoment(
                        "Introducción.",
                        List.of(new AlbumLogBestMomentItem("Momento", "Descripción.")),
                        "Conclusión."
                ),
                List.of("viernes a la noche", "fin de semana"),
                "Nota editorial",
                "Importancia",
                "Editorial note",
                "Recommended if",
                "Avoid if",
                "Album context",
                List.of(),
                spotifyAlbumId
        );
    }
}
