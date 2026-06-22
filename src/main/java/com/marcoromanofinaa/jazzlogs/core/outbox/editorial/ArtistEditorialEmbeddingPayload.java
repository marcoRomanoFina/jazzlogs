package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import java.time.Instant;
import java.util.UUID;

public record ArtistEditorialEmbeddingPayload(
        String artistNodeId,
        UUID requestedByUserId,
        Instant requestedAt
) {
}
