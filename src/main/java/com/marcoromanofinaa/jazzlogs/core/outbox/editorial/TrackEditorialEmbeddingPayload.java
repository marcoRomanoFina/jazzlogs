package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import java.time.Instant;
import java.util.UUID;

public record TrackEditorialEmbeddingPayload(
        String trackNodeId,
        UUID requestedByUserId,
        Instant requestedAt
) {
}
