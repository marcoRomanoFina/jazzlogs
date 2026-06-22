package com.marcoromanofinaa.jazzlogs.chat.usage.config;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordStage;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.chat.usage")
public record UsageLimitProperties(
        Map<Plan, Long> monthlyCreditLimit,
        Map<AIModelType, Long> minimumBalanceRequiredCredits,
        Map<UsageRecordStage, Long> stageCreditCost,
        String pricingVersion,
        Map<String, ModelPricing> modelPricing,
        Duration periodDuration
) {

    public long monthlyCreditLimitFor(Plan plan) {
        return monthlyCreditLimit().getOrDefault(plan, 0L);
    }

    public long minimumBalanceRequiredCreditsFor(AIModelType modelType) {
        return minimumBalanceRequiredCredits()
                .getOrDefault(modelType, 1L);
    }

    public long creditCostFor(UsageRecordStage stage) {
        return stageCreditCost().getOrDefault(stage, 1L);
    }

    public ModelPricing pricingFor(String providerModelName) {
        if (providerModelName == null || providerModelName.isBlank()) {
            return null;
        }
        return modelPricing().get(providerModelName);
    }

    public record ModelPricing(
            long inputMicrosUsdPerMillionTokens,
            long cachedInputMicrosUsdPerMillionTokens,
            long outputMicrosUsdPerMillionTokens
    ) {
    }
}
