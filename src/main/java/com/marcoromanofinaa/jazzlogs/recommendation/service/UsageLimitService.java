package com.marcoromanofinaa.jazzlogs.recommendation.service;

import com.marcoromanofinaa.jazzlogs.recommendation.AIModelType;
import com.marcoromanofinaa.jazzlogs.recommendation.exception.UsageLimitExceededException;
import com.marcoromanofinaa.jazzlogs.chat.usage.config.UsageLimitProperties;
import com.marcoromanofinaa.jazzlogs.user.subscription.service.UserSubscriptionService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UsageLimitService {

    private final UsageLimitProperties usageLimitProperties;
    private final UserSubscriptionService userSubscriptionService;

    @Transactional(readOnly = true)
    public void validateCreditBalance(UUID userId, AIModelType requestedModel) {
        var subscription = userSubscriptionService.requireActiveSubscription(userId);
        var minimumBalanceRequiredCredits = usageLimitProperties.minimumBalanceRequiredCreditsFor(requestedModel);

        if (subscription.getCreditsRemaining() == null || subscription.getCreditsRemaining() <= 0) {
            throw new UsageLimitExceededException(userId, requestedModel);
        }

        if (subscription.getCreditsRemaining() < minimumBalanceRequiredCredits) {
            throw new UsageLimitExceededException(
                    userId,
                    requestedModel,
                    "Not enough quota remaining to start a request for model %s".formatted(requestedModel)
            );
        }
    }
}
