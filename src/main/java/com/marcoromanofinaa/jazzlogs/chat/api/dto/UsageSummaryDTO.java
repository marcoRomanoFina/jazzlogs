package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.time.Instant;

public record UsageSummaryDTO(
        Long tokenLimit,
        Long tokensUsed,
        Long tokensRemaining,
        Double remainingPercentage,
        Instant periodStart,
        Instant periodEnd
) {
}
