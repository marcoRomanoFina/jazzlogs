package com.marcoromanofinaa.jazzlogs.chat.usage.pricing;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.usage.config.UsageLimitProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UsagePricingService {

    private static final double TOKENS_PER_MILLION = 1_000_000.0;

    private final UsageLimitProperties usageLimitProperties;

    public UsageCharge calculateCharge(ModelUsage usage) {
        var pricing = usageLimitProperties.pricingFor(usage.providerModelName());
        if (pricing == null) {
            throw new IllegalArgumentException(
                    "Missing pricing configuration for model " + usage.providerModelName()
            );
        }

        long inputCostMicrosUsd = componentCostMicrosUsd(usage.inputTokens(), pricing.inputMicrosUsdPerMillionTokens());
        long cachedInputCostMicrosUsd = componentCostMicrosUsd(
                usage.cachedInputTokens(),
                pricing.cachedInputMicrosUsdPerMillionTokens()
        );
        long outputCostMicrosUsd = componentCostMicrosUsd(usage.outputTokens(), pricing.outputMicrosUsdPerMillionTokens());

        return new UsageCharge(
                inputCostMicrosUsd + cachedInputCostMicrosUsd + outputCostMicrosUsd,
                usageLimitProperties.pricingVersion()
        );
    }

    private long componentCostMicrosUsd(Integer tokens, long priceMicrosUsdPerMillionTokens) {
        int safeTokens = tokens == null ? 0 : Math.max(tokens, 0);
        if (safeTokens == 0 || priceMicrosUsdPerMillionTokens == 0) {
            return 0L;
        }
        return Math.round((safeTokens * priceMicrosUsdPerMillionTokens) / TOKENS_PER_MILLION);
    }

    public record UsageCharge(
            long costMicrosUsd,
            String pricingVersion
    ) {
    }
}
