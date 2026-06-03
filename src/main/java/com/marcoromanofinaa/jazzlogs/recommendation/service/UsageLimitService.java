package com.marcoromanofinaa.jazzlogs.recommendation.service;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.UsageLimitExceededException;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordRepository;
import com.marcoromanofinaa.jazzlogs.chat.usage.config.UsageLimitProperties;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageLimitService {

    private final UsageRecordRepository usageRecordRepository;
    private final UsageLimitProperties usageLimitProperties;
    private final UserSubscriptionService userSubscriptionService;

    @Transactional(readOnly = true)
    public void validateTokenBalance(UUID userId, AIModelType requestedModel) {
        var subscription = userSubscriptionService.requireActiveSubscription(userId);
        var currentPlan = subscription.getPlan();
        var minimumBalanceRequiredTokens = usageLimitProperties.minimumBalanceRequiredTokensFor(requestedModel);

        if (subscription.getTokensRemaining() == null || subscription.getTokensRemaining() <= 0) {
            throw new UsageLimitExceededException(userId, requestedModel);
        }

        if (subscription.getTokensRemaining() < minimumBalanceRequiredTokens) {
            throw new UsageLimitExceededException(
                    userId,
                    requestedModel,
                    "Not enough quota remaining to start a request for model %s".formatted(requestedModel)
            );
        }

        var consumedInCurrentPeriodTokens = usageRecordRepository.sumTotalTokensByUserIdAndCreatedAtBetween(
                userId,
                subscription.getPeriodStart(),
                subscription.getPeriodEnd()
        );
        var tokenLimit = usageLimitProperties.monthlyTokenLimitFor(currentPlan);

        if (consumedInCurrentPeriodTokens >= tokenLimit) {
            throw new UsageLimitExceededException(
                    userId,
                    requestedModel,
                    "Usage limit exceeded for the current plan period"
            );
        }
    }
}
