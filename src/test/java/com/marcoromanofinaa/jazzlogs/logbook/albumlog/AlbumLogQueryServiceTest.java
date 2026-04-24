package com.marcoromanofinaa.jazzlogs.logbook.albumlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class AlbumLogQueryServiceTest {

    @Test
    void returnsAllAlbumLogs() {
        var repository = repository(
                List.of(
                        albumLog(1, "Spunky"),
                        albumLog(2, "Cornbread")
                )
        );
        var service = new AlbumLogQueryService(repository);

        var responses = service.findAll();

        assertThat(responses).extracting(AlbumLogResponse::logNumber).containsExactly(1, 2);
        assertThat(responses.getFirst().album()).isEqualTo("Spunky");
    }

    @Test
    void returnsAlbumLogByLogNumber() {
        var repository = repository(List.of(albumLog(7, "Sonny Rollins and the Contemporary Leaders")));
        var service = new AlbumLogQueryService(repository);

        var response = service.findByLogNumber(7);

        assertThat(response.logNumber()).isEqualTo(7);
        assertThat(response.album()).isEqualTo("Sonny Rollins and the Contemporary Leaders");
        assertThat(response.spotifyAlbumSeedId()).isNull();
        assertThat(response.spotifyAlbumId()).isNull();
    }

    @Test
    void throwsWhenAlbumLogDoesNotExist() {
        var service = new AlbumLogQueryService(repository(List.of()));

        assertThatThrownBy(() -> service.findByLogNumber(404))
                .isInstanceOf(AlbumLogNotFoundException.class)
                .hasMessageContaining("404");
    }

    private AlbumLogRepository repository(List<AlbumLog> storage) {
        return (AlbumLogRepository) Proxy.newProxyInstance(
                AlbumLogRepository.class.getClassLoader(),
                new Class<?>[]{AlbumLogRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByOrderByLogNumberAsc" -> storage;
                    case "findByLogNumber" -> storage.stream()
                            .filter(albumLog -> albumLog.getLogNumber().equals(args[0]))
                            .findFirst();
                    case "toString" -> "AlbumLogQueryRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                }
        );
    }

    private AlbumLog albumLog(int logNumber, String album) {
        return AlbumLog.create(new AlbumLogData(
                logNumber,
                album,
                "Test Artist",
                "Test caption",
                LocalDate.of(2026, 4, 15),
                "https://www.instagram.com/p/TEST123/",
                "Hard Bop",
                null,
                new String[]{"warm", "groovy"},
                null,
                new String[]{},
                null,
                null,
                null,
                null,
                new String[]{},
                "Test notes",
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null
        ));
    }
}
