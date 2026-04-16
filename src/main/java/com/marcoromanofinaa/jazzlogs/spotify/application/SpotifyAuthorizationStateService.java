package com.marcoromanofinaa.jazzlogs.spotify.application;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
// Holds short-lived OAuth state values so the Spotify callback can be verified
// against an authorization flow initiated by this backend.
public class SpotifyAuthorizationStateService {

    private static final Duration STATE_TTL = Duration.ofMinutes(10);

    private final Map<String, Instant> validStates = new ConcurrentHashMap<>();

    // Issues a fresh random state, stores it with an expiry, and returns it to
    // be sent in the Spotify authorization URL.
    public String issueState() {
        purgeExpiredStates();

        var state = UUID.randomUUID().toString();
        validStates.put(state, Instant.now().plus(STATE_TTL));
        return state;
    }

    // Verifies that the callback state exists, has not expired, and can only be
    // used once by removing it from the in-memory store.
    public boolean consume(String state) {
        purgeExpiredStates();
        var expiresAt = validStates.remove(state);
        return expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    // Keeps the in-memory state store small and prevents old OAuth states from
    // being accepted after their TTL.
    private void purgeExpiredStates() {
        var now = Instant.now();
        validStates.entrySet().removeIf(entry -> entry.getValue().isBefore(now));
    }
}
