package com.marcoromanofinaa.jazzlogs.chat.api.dto;

import java.time.Instant;

public record UsageSummaryDTO(
        Long creditLimit,
        Long creditsUsed,
        Long creditsRemaining,
        Double remainingPercentage,
        Instant periodStart,
        Instant periodEnd
) {
}
