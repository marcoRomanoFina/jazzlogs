package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import java.time.Instant;
import java.util.UUID;

public record ArtistLogPayload(
        UUID artistLogId,
        UUID requestedByUserId,
        Instant requestedAt
) {
}
