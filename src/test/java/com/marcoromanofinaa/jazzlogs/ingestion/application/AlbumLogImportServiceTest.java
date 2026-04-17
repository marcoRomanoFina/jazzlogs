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
                    "logNumber": 99,
                    "moods": ["warm", "groovy"],
                    "notes": "Standout track: Test Track.",
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
        assertThat(savedAlbumLog.getMoods()).containsExactly("warm", "groovy");
        assertThat(savedAlbumLog.getNotes()).isEqualTo("Standout track: Test Track.");
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
                    "logNumber": 99,
                    "moods": ["warm", "groovy"],
                    "notes": "Updated notes.",
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
