package com.marcoromanofinaa.jazzlogs.chat.usage;

import static org.assertj.core.api.Assertions.assertThat;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsageRecordTest {

    @Test
    void createStoresModelAndCachedInputTokens() {
        var now = Instant.parse("2026-05-27T20:00:00Z");

        var record = UsageRecord.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UsageRecordStage.ROUTER,
                AIModelType.BASIC,
                "gpt-5.4-nano",
                120,
                45,
                30,
                84L,
                "2026-05-28",
                now
        );

        assertThat(record.getModelUsed()).isEqualTo(AIModelType.BASIC);
        assertThat(record.getProviderModelName()).isEqualTo("gpt-5.4-nano");
        assertThat(record.getStage()).isEqualTo(UsageRecordStage.ROUTER);
        assertThat(record.getInputTokens()).isEqualTo(120);
        assertThat(record.getCachedInputTokens()).isEqualTo(45);
        assertThat(record.getOutputTokens()).isEqualTo(30);
        assertThat(record.getTotalTokens()).isEqualTo(150);
        assertThat(record.getCostMicrosUsd()).isEqualTo(84L);
        assertThat(record.getPricingVersion()).isEqualTo("2026-05-28");
        assertThat(record.getCreatedAt()).isEqualTo(now);
    }

    @Test
    void createDefaultsMissingTokenCountsToZero() {
        var record = UsageRecord.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UsageRecordStage.BASIC_RECOMMENDATION,
                AIModelType.BASIC,
                "gpt-5.4-mini",
                null,
                null,
                null,
                null,
                null,
                Instant.now()
        );

        assertThat(record.getInputTokens()).isZero();
        assertThat(record.getCachedInputTokens()).isZero();
        assertThat(record.getOutputTokens()).isZero();
        assertThat(record.getTotalTokens()).isZero();
        assertThat(record.getCostMicrosUsd()).isZero();
        assertThat(record.getPricingVersion()).isEqualTo("unknown");
    }
}
