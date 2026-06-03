package com.marcoromanofinaa.jazzlogs.chat.usage;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usage_records")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UsageRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "chat_session_id", nullable = false)
    private UUID chatSessionId;

    @Column(name = "chat_exchange_id", nullable = false)
    private UUID chatExchangeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage", nullable = false)
    private UsageRecordStage stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_used", nullable = false)
    private AIModelType modelUsed;

    @Column(name = "provider_model_name", nullable = false)
    private String providerModelName;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "cached_input_tokens", nullable = false)
    private Integer cachedInputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    @Column(name = "cost_micros_usd", nullable = false)
    private Long costMicrosUsd;

    @Column(name = "pricing_version", nullable = false)
    private String pricingVersion;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public static UsageRecord create(
            UUID userId,
            UUID chatSessionId,
            UUID chatExchangeId,
            UsageRecordStage stage,
            AIModelType modelUsed,
            String providerModelName,
            Integer inputTokens,
            Integer cachedInputTokens,
            Integer outputTokens,
            Long costMicrosUsd,
            String pricingVersion,
            Instant now
    ) {
        int safeInput = inputTokens == null ? 0 : inputTokens;
        int safeCachedInput = cachedInputTokens == null ? 0 : cachedInputTokens;
        int safeOutput = outputTokens == null ? 0 : outputTokens;
        long safeCostMicrosUsd = costMicrosUsd == null ? 0L : Math.max(costMicrosUsd, 0L);

        var record = new UsageRecord();
        record.userId = userId;
        record.chatSessionId = chatSessionId;
        record.chatExchangeId = chatExchangeId;
        record.stage = stage;
        record.modelUsed = modelUsed;
        record.providerModelName = providerModelName;
        record.inputTokens = safeInput;
        record.cachedInputTokens = safeCachedInput;
        record.outputTokens = safeOutput;
        record.totalTokens = safeInput + safeOutput;
        record.costMicrosUsd = safeCostMicrosUsd;
        record.pricingVersion = pricingVersion == null || pricingVersion.isBlank() ? "unknown" : pricingVersion;
        record.createdAt = now;
        return record;
    }
}
