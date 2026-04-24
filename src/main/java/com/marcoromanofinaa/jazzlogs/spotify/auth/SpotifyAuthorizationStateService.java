package com.marcoromanofinaa.jazzlogs.spotify.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
// Guarda estados OAuth de vida corta para verificar que el callback de Spotify
// corresponde a un flujo de autorización iniciado por este backend.
public class SpotifyAuthorizationStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final Map<String, Instant> validStates = new ConcurrentHashMap<>();

    // Genera un state aleatorio nuevo, lo guarda con vencimiento y lo devuelve
    // para enviarlo en la URL de autorización de Spotify.
    public String issueState() {
        purgeExpiredStates();

        var state = UUID.randomUUID().toString();
        validStates.put(state, Instant.now().plus(STATE_TTL));
        log.debug("Issued Spotify OAuth state. activeStates={}", validStates.size());
        return state;
    }

    // Verifica que el state del callback exista, no haya expirado y sólo pueda
    // usarse una vez eliminándolo del store en memoria.
    public boolean consume(String state) {
        purgeExpiredStates();
        var expiresAt = validStates.remove(state);
        var valid = expiresAt != null && expiresAt.isAfter(Instant.now());
        log.debug("Consumed Spotify OAuth state valid={} activeStates={}", valid, validStates.size());
        return valid;
    }

    // Mantiene chico el store en memoria y evita que estados OAuth viejos sean
    // aceptados después de su TTL.
    private void purgeExpiredStates() {
        var now = Instant.now();
        validStates.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
