package com.marcoromanofinaa.jazzlogs.chat.usage.config;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jazzlogs.chat.usage")
public record UsageLimitProperties(
        Map<Plan, Long> monthlyTokenLimit,
        Map<AIModelType, Long> minimumBalanceRequiredTokens,
        String pricingVersion,
        Map<String, ModelPricing> modelPricing,
        Duration periodDuration
) {

    public long monthlyTokenLimitFor(Plan plan) {
        return monthlyTokenLimit().getOrDefault(plan, 0L);
    }

    public long minimumBalanceRequiredTokensFor(AIModelType modelType) {
        return minimumBalanceRequiredTokens()
                .getOrDefault(modelType, 1L);
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
