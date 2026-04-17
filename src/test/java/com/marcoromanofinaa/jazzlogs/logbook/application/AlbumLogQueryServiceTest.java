package com.marcoromanofinaa.jazzlogs.logbook.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marcoromanofinaa.jazzlogs.logbook.domain.AlbumLog;
import com.marcoromanofinaa.jazzlogs.logbook.infrastructure.AlbumLogRepository;
import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AlbumLogQueryServiceTest {

    @Test
    void returnsAllAlbumLogsOrderedByLogNumber() {
        var repository = repository(
                Map.of(
                        2, albumLog(2, "Cornbread"),
                        1, albumLog(1, "Spunky")
                )
        );
        var service = new AlbumLogQueryService(repository);

        var responses = service.findAll();

        assertThat(responses).extracting(AlbumLogResponse::logNumber).containsExactly(1, 2);
        assertThat(responses.getFirst().album()).isEqualTo("Spunky");
    }

    @Test
    void returnsAlbumLogByLogNumber() {
        var repository = repository(Map.of(7, albumLog(7, "Sonny Rollins and the Contemporary Leaders")));
        var service = new AlbumLogQueryService(repository);

        var response = service.findByLogNumber(7);

        assertThat(response.logNumber()).isEqualTo(7);
        assertThat(response.album()).isEqualTo("Sonny Rollins and the Contemporary Leaders");
        assertThat(response.spotifyAlbumSeedId()).isNull();
        assertThat(response.spotifyAlbumId()).isNull();
    }

    @Test
    void throwsWhenAlbumLogDoesNotExist() {
        var service = new AlbumLogQueryService(repository(Map.of()));

        assertThatThrownBy(() -> service.findByLogNumber(404))
                .isInstanceOf(AlbumLogNotFoundException.class)
                .hasMessageContaining("404");
    }

    private AlbumLogRepository repository(Map<Integer, AlbumLog> storage) {
        return (AlbumLogRepository) Proxy.newProxyInstance(
                AlbumLogRepository.class.getClassLoader(),
                new Class<?>[]{AlbumLogRepository.class},
                (proxy, method, args) -> switch (method.getName()) {
                    case "findAllByOrderByLogNumberAsc" -> storage.values().stream()
                            .sorted(java.util.Comparator.comparing(AlbumLog::getLogNumber))
                            .toList();
                    case "findByLogNumber" -> Optional.ofNullable(storage.get((Integer) args[0]));
                    case "toString" -> "AlbumLogQueryRepository";
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    default -> throw new UnsupportedOperationException("Method not supported in test: " + method.getName());
                }
        );
    }

    private AlbumLog albumLog(int logNumber, String album) {
        return AlbumLog.create(
                logNumber,
                album,
                "Test Artist",
                "Test caption",
                LocalDate.of(2026, 4, 15),
                "https://www.instagram.com/p/TEST123/",
                "Hard Bop",
                new String[]{"warm", "groovy"},
                "Test notes",
                null
        );
    }
}
