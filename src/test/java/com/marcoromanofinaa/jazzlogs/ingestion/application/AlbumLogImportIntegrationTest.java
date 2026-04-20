package com.marcoromanofinaa.jazzlogs.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
class AlbumLogImportIntegrationTest {

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
    }

    @Autowired
    private AlbumLogImportService albumLogImportService;

    @Autowired
    private AlbumLogRepository albumLogRepository;

    @TempDir
    Path tempDir;

    @Test
    void importsAlbumLogsIntoRealPostgresDatabase() throws Exception {
        var json = """
                [
                  {
                    "album": "Integration Album",
                    "artist": "Integration Artist",
                    "caption": "Integration caption",
                    "postedAt": "2026-04-15",
                    "instagramPermalink": "https://www.instagram.com/p/INTEGRATION123/",
                    "style": "Hard Bop / Soul Jazz",
                    "releaseYear": "1965",
                    "logNumber": 501,
                    "moods": ["warm", "groovy"],
                    "tier": "essential",
                    "vibe": ["warm", "groovy"],
                    "energy": "high",
                    "moodIntensity": "medium",
                    "accessibility": "easy",
                    "bestMoment": "Integration moment",
                    "listeningContext": ["integration"],
                    "notes": "Standout track: Integration Track.",
                    "whyItMatters": "Integration why.",
                    "editorialNote": "Integration editorial.",
                    "recommendedIf": "Integration recommended.",
                    "avoidIf": "Integration avoid.",
                    "albumContext": "Integration context.",
                    "personnel": [
                      {
                        "name": "Integration Musician",
                        "role": "piano"
                      }
                    ],
                    "spotifyAlbumId": "SPOTIFY501"
                  }
                ]
                """;

        var path = tempDir.resolve("albums.json");
        Files.writeString(path, json);

        var importedCount = albumLogImportService.importFromJson(path);

        assertThat(importedCount).isEqualTo(1);

        var savedAlbumLog = albumLogRepository.findAllByLogNumberIn(java.util.List.of(501))
                .stream()
                .findFirst()
                .orElseThrow();
        assertThat(savedAlbumLog.getAlbum()).isEqualTo("Integration Album");
        assertThat(savedAlbumLog.getArtist()).isEqualTo("Integration Artist");
        assertThat(savedAlbumLog.getCaption()).isEqualTo("Integration caption");
        assertThat(savedAlbumLog.getPostedAt()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(savedAlbumLog.getInstagramPermalink()).isEqualTo("https://www.instagram.com/p/INTEGRATION123/");
        assertThat(savedAlbumLog.getStyle()).isEqualTo("Hard Bop / Soul Jazz");
        assertThat(savedAlbumLog.getReleaseYear()).isEqualTo("1965");
        assertThat(savedAlbumLog.getMoods()).containsExactly("warm", "groovy");
        assertThat(savedAlbumLog.getTier()).isEqualTo("essential");
        assertThat(savedAlbumLog.getVibe()).containsExactly("warm", "groovy");
        assertThat(savedAlbumLog.getEnergy()).isEqualTo("high");
        assertThat(savedAlbumLog.getMoodIntensity()).isEqualTo("medium");
        assertThat(savedAlbumLog.getAccessibility()).isEqualTo("easy");
        assertThat(savedAlbumLog.getBestMoment()).isEqualTo("Integration moment");
        assertThat(savedAlbumLog.getListeningContext()).containsExactly("integration");
        assertThat(savedAlbumLog.getNotes()).isEqualTo("Standout track: Integration Track.");
        assertThat(savedAlbumLog.getWhyItMatters()).isEqualTo("Integration why.");
        assertThat(savedAlbumLog.getEditorialNote()).isEqualTo("Integration editorial.");
        assertThat(savedAlbumLog.getRecommendedIf()).isEqualTo("Integration recommended.");
        assertThat(savedAlbumLog.getAvoidIf()).isEqualTo("Integration avoid.");
        assertThat(savedAlbumLog.getAlbumContext()).isEqualTo("Integration context.");
        assertThat(savedAlbumLog.getPersonnel())
                .singleElement()
                .satisfies(person -> {
                    assertThat(person.name()).isEqualTo("Integration Musician");
                    assertThat(person.role()).isEqualTo("piano");
                });
        assertThat(savedAlbumLog.getSpotifyAlbumSeedId()).isEqualTo("SPOTIFY501");
    }
}
