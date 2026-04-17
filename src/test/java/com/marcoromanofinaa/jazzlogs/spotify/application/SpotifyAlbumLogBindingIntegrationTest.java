package com.marcoromanofinaa.jazzlogs.spotify.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import com.marcoromanofinaa.jazzlogs.spotify.domain.SpotifyAlbum;
import com.marcoromanofinaa.jazzlogs.spotify.infrastructure.SpotifyAlbumRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@org.junit.jupiter.api.condition.EnabledIfSystemProperty(named = "runTestcontainers", matches = "true")
@SpringBootTest
@SuppressWarnings("resource")
class SpotifyAlbumLogBindingIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg17")
            .withDatabaseName("jazzlogs")
            .withUsername("jazzlogs")
            .withPassword("jazzlogs");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.ai.openai.api-key", () -> "");
        registry.add("jazzlogs.ingestion.json.enabled", () -> "false");
        registry.add("jazzlogs.spotify.playlist-id", () -> "playlist-1");
    }

    @Autowired
    private SpotifyAlbumLogBindingService bindingService;

    @Autowired
    private SpotifyAlbumRepository spotifyAlbumRepository;

    @Autowired
    private AlbumLogRepository albumLogRepository;

    @Test
    void bindsAlbumLogToSyncedSpotifyAlbumUsingSeedId() {
        var album = spotifyAlbumRepository.save(
                SpotifyAlbum.builder()
                        .spotifyAlbumId("album-1")
                        .sourcePlaylistId("playlist-1")
                        .name("Bound Album")
                        .spotifyUrl("https://open.spotify.com/album/album-1")
                        .build()
        );
        var albumLog = albumLogRepository.save(
                AlbumLog.create(
                        77,
                        "Bound Album",
                        "Bound Artist",
                        "Caption",
                        LocalDate.of(2026, 4, 17),
                        "https://www.instagram.com/p/BINDING123/",
                        "Hard Bop",
                        new String[]{"warm"},
                        "Notes",
                        "album-1"
                )
        );

        var boundCount = bindingService.bindConfiguredPlaylistAlbumsToLogs();

        assertThat(boundCount).isEqualTo(1);
        var reloaded = albumLogRepository.findByLogNumber(albumLog.getLogNumber()).orElseThrow();
        assertThat(reloaded.getSpotifyAlbum()).isNotNull();
        assertThat(reloaded.getSpotifyAlbum().getSpotifyAlbumId()).isEqualTo(album.getSpotifyAlbumId());
    }
}
