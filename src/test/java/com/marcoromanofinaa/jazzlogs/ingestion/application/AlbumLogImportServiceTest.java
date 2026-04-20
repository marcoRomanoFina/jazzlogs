package com.marcoromanofinaa.jazzlogs.ingestion.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AlbumLogImportServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void importsValidAlbumLogsFromJson() throws Exception {
        var storage = new HashMap<Integer, AlbumLog>();
        var repository = inMemoryRepository(storage);
        var service = new AlbumLogImportService(
                JsonMapper.builder().findAndAddModules().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                repository
        );

        var json = """
                [
                  {
                    "album": "Test Album",
                    "artist": "Test Artist",
                    "caption": "A valid caption for testing",
                    "postedAt": "2026-04-15",
                    "instagramPermalink": "https://www.instagram.com/p/TEST123/",
                    "style": "Hard Bop",
                    "releaseYear": "1965",
                    "logNumber": 99,
                    "moods": ["warm", "groovy"],
                    "tier": "essential",
                    "vibe": ["cálido", "groovy"],
                    "energy": "high",
                    "moodIntensity": "medium",
                    "accessibility": "easy",
                    "bestMoment": "Friday night",
                    "listeningContext": ["friday-night", "vinyl-session"],
                    "notes": "Standout track: Test Track.",
                    "whyItMatters": "Important test context.",
                    "editorialNote": "Editorial test note.",
                    "recommendedIf": "Recommended for tests.",
                    "avoidIf": "Avoid if tests are too slow.",
                    "albumContext": "Historical test context.",
                    "personnel": [
                      {
                        "name": "Test Musician",
                        "role": "piano"
                      }
                    ],
                    "spotifyAlbumId": "TEST123"
                  }
                ]
                """;

        var path = writeSeedFile(json);

        var importedCount = service.importFromJson(path);

        assertThat(importedCount).isEqualTo(1);
        assertThat(storage).hasSize(1);

        var savedAlbumLog = storage.get(99);
        assertThat(savedAlbumLog.getLogNumber()).isEqualTo(99);
        assertThat(savedAlbumLog.getAlbum()).isEqualTo("Test Album");
        assertThat(savedAlbumLog.getArtist()).isEqualTo("Test Artist");
        assertThat(savedAlbumLog.getCaption()).isEqualTo("A valid caption for testing");
        assertThat(savedAlbumLog.getPostedAt()).isEqualTo(LocalDate.of(2026, 4, 15));
        assertThat(savedAlbumLog.getInstagramPermalink()).isEqualTo("https://www.instagram.com/p/TEST123/");
        assertThat(savedAlbumLog.getStyle()).isEqualTo("Hard Bop");
        assertThat(savedAlbumLog.getReleaseYear()).isEqualTo("1965");
        assertThat(savedAlbumLog.getMoods()).containsExactly("warm", "groovy");
        assertThat(savedAlbumLog.getTier()).isEqualTo("essential");
        assertThat(savedAlbumLog.getVibe()).containsExactly("cálido", "groovy");
        assertThat(savedAlbumLog.getEnergy()).isEqualTo("high");
        assertThat(savedAlbumLog.getMoodIntensity()).isEqualTo("medium");
        assertThat(savedAlbumLog.getAccessibility()).isEqualTo("easy");
        assertThat(savedAlbumLog.getBestMoment()).isEqualTo("Friday night");
        assertThat(savedAlbumLog.getListeningContext()).containsExactly("friday-night", "vinyl-session");
        assertThat(savedAlbumLog.getNotes()).isEqualTo("Standout track: Test Track.");
        assertThat(savedAlbumLog.getWhyItMatters()).isEqualTo("Important test context.");
        assertThat(savedAlbumLog.getEditorialNote()).isEqualTo("Editorial test note.");
        assertThat(savedAlbumLog.getRecommendedIf()).isEqualTo("Recommended for tests.");
        assertThat(savedAlbumLog.getAvoidIf()).isEqualTo("Avoid if tests are too slow.");
        assertThat(savedAlbumLog.getAlbumContext()).isEqualTo("Historical test context.");
        assertThat(savedAlbumLog.getPersonnel())
                .singleElement()
                .satisfies(person -> {
                    assertThat(person.name()).isEqualTo("Test Musician");
                    assertThat(person.role()).isEqualTo("piano");
                });
        assertThat(savedAlbumLog.getSpotifyAlbumSeedId()).isEqualTo("TEST123");
    }

    @Test
    void throwsWhenSeedViolatesValidationRules() throws Exception {
        var storage = new HashMap<Integer, AlbumLog>();
        var repository = inMemoryRepository(storage);
        var service = new AlbumLogImportService(
                JsonMapper.builder().findAndAddModules().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                repository
        );

        var json = """
                [
                  {
                    "album": "Broken Album",
                    "artist": "Broken Artist",
                    "caption": "Still valid caption",
                    "postedAt": "2026-04-15",
                    "instagramPermalink": "https://example.com/not-instagram",
                    "style": "Hard Bop",
                    "logNumber": 100,
                    "moods": ["warm"],
                    "notes": "Broken permalink.",
                    "spotifyAlbumId": "BROKEN123"
                  }
                ]
                """;

        var path = writeSeedFile(json);

        assertThatThrownBy(() -> service.importFromJson(path))
                .isInstanceOf(ConstraintViolationException.class)
                .hasMessageContaining("instagramPermalink");

        assertThat(storage).isEmpty();
    }

    @Test
    void updatesExistingAlbumLogWhenLogNumberAlreadyExists() throws Exception {
        var existingAlbumLog = AlbumLog.create(
                99,
                "Old Album",
                "Old Artist",
                "Old caption",
                LocalDate.of(2026, 1, 1),
                "https://www.instagram.com/p/OLD123/",
                "Old Style",
                new String[]{"old"},
                "Old notes",
                null
        );
        var storage = new HashMap<Integer, AlbumLog>();
        storage.put(99, existingAlbumLog);
        var repository = inMemoryRepository(storage);
        var service = new AlbumLogImportService(
                JsonMapper.builder().findAndAddModules().build(),
                Validation.buildDefaultValidatorFactory().getValidator(),
                repository
        );

        var json = """
                [
                  {
                    "album": "Updated Album",
                    "artist": "Updated Artist",
                    "caption": "Updated caption",
                    "postedAt": "2026-04-15",
                    "instagramPermalink": "https://www.instagram.com/p/UPDATED123/",
                    "style": "Hard Bop",
                    "releaseYear": "1966",
                    "logNumber": 99,
                    "moods": ["warm", "groovy"],
                    "tier": "deep_cut",
                    "vibe": ["nocturno"],
                    "energy": "medium",
                    "moodIntensity": "high",
                    "accessibility": "medium",
                    "bestMoment": "Updated moment",
                    "listeningContext": ["late-night"],
                    "notes": "Updated notes.",
                    "whyItMatters": "Updated why.",
                    "editorialNote": "Updated editorial.",
                    "recommendedIf": "Updated recommended.",
                    "avoidIf": "Updated avoid.",
                    "albumContext": "Updated context.",
                    "personnel": [
                      {
                        "name": "Updated Musician",
                        "role": "bass"
                      }
                    ],
                    "spotifyAlbumId": "UPDATED123"
                  }
                ]
                """;

        var path = writeSeedFile(json);

        var importedCount = service.importFromJson(path);

        assertThat(importedCount).isEqualTo(1);
        assertThat(storage).hasSize(1);
        assertThat(storage.get(99)).isSameAs(existingAlbumLog);
        assertThat(existingAlbumLog.getAlbum()).isEqualTo("Updated Album");
        assertThat(existingAlbumLog.getArtist()).isEqualTo("Updated Artist");
        assertThat(existingAlbumLog.getCaption()).isEqualTo("Updated caption");
        assertThat(existingAlbumLog.getInstagramPermalink()).isEqualTo("https://www.instagram.com/p/UPDATED123/");
        assertThat(existingAlbumLog.getReleaseYear()).isEqualTo("1966");
        assertThat(existingAlbumLog.getTier()).isEqualTo("deep_cut");
        assertThat(existingAlbumLog.getVibe()).containsExactly("nocturno");
        assertThat(existingAlbumLog.getListeningContext()).containsExactly("late-night");
        assertThat(existingAlbumLog.getPersonnel())
                .singleElement()
                .satisfies(person -> {
                    assertThat(person.name()).isEqualTo("Updated Musician");
                    assertThat(person.role()).isEqualTo("bass");
                });
        assertThat(existingAlbumLog.getSpotifyAlbumSeedId()).isEqualTo("UPDATED123");
    }

    private Path writeSeedFile(String json) throws Exception {
        var path = tempDir.resolve("albums.json");
        Files.writeString(path, json);
        return path;
    }

    @SuppressWarnings("unchecked")
    private AlbumLogRepository inMemoryRepository(Map<Integer, AlbumLog> storage) {
        return (AlbumLogRepository) Proxy.newProxyInstance(
                AlbumLogRepository.class.getClassLoader(),
                new Class<?>[]{AlbumLogRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByLogNumberIn" -> ((Collection<Integer>) args[0]).stream()
                            .map(storage::get)
                            .filter(java.util.Objects::nonNull)
                            .toList();
                    case "save" -> {
                        var albumLog = (AlbumLog) args[0];
                        storage.put(albumLog.getLogNumber(), albumLog);
                        yield albumLog;
                    }
                    case "saveAll" -> {
                        var albumLogs = (Iterable<AlbumLog>) args[0];
                        var savedAlbumLogs = new java.util.ArrayList<AlbumLog>();
                        for (var albumLog : albumLogs) {
                            storage.put(albumLog.getLogNumber(), albumLog);
                            savedAlbumLogs.add(albumLog);
                        }
                        yield savedAlbumLogs;
                    }
                    case "toString" -> "InMemoryAlbumLogRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                }
        );
    }
}
