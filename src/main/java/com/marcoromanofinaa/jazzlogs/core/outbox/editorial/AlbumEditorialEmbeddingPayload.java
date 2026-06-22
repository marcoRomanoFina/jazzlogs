package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import java.time.Instant;
import java.util.UUID;

public record AlbumEditorialEmbeddingPayload(
        String albumNodeId,
        UUID requestedByUserId,
        Instant requestedAt
) {
}
