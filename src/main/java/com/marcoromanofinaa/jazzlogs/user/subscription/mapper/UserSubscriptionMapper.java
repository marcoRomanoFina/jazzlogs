package com.marcoromanofinaa.jazzlogs.user.subscription.mapper;

import com.marcoromanofinaa.jazzlogs.chat.api.dto.UsageSummaryDTO;
import com.marcoromanofinaa.jazzlogs.user.subscription.model.UserSubscription;
import org.springframework.stereotype.Component;

@Component
public class UserSubscriptionMapper {

    public UsageSummaryDTO toUsageSummary(UserSubscription subscription) {
        return new UsageSummaryDTO(
                subscription.getCreditLimit(),
                subscription.getCreditsUsed(),
                subscription.getCreditsRemaining(),
                subscription.remainingPercentage(),
                subscription.getPeriodStart(),
                subscription.getPeriodEnd()
        );
    }
}
