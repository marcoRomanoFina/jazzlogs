package com.marcoromanofinaa.jazzlogs.core.outbox.editorial;

import java.time.Instant;
import java.util.UUID;

public record AlbumLogPayload(
        UUID albumLogId,
        UUID requestedByUserId,
        Instant requestedAt
) {
}
