package com.marcoromanofinaa.jazzlogs.user.subscription.service;

import com.marcoromanofinaa.jazzlogs.chat.usage.ModelUsage;
import com.marcoromanofinaa.jazzlogs.chat.api.dto.UsageSummaryDTO;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecord;
import com.marcoromanofinaa.jazzlogs.chat.usage.UsageRecordRepository;
import com.marcoromanofinaa.jazzlogs.chat.usage.config.UsageLimitProperties;
import com.marcoromanofinaa.jazzlogs.chat.usage.pricing.UsagePricingService;
import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import com.marcoromanofinaa.jazzlogs.user.exception.UserNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.repository.UserRepository;
import com.marcoromanofinaa.jazzlogs.user.subscription.mapper.UserSubscriptionMapper;
import com.marcoromanofinaa.jazzlogs.user.subscription.exception.UserSubscriptionExpiredException;
import com.marcoromanofinaa.jazzlogs.user.subscription.exception.UserSubscriptionNotFoundException;
import com.marcoromanofinaa.jazzlogs.user.subscription.model.UserSubscription;
import com.marcoromanofinaa.jazzlogs.user.subscription.repository.UserSubscriptionRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserSubscriptionService {

    private final UserSubscriptionRepository userSubscriptionRepository;
    private final UserRepository userRepository;
    private final UsageRecordRepository usageRecordRepository;
    private final UsageLimitProperties usageLimitProperties;
    private final UsagePricingService usagePricingService;
    private final UserSubscriptionMapper userSubscriptionMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public UserSubscription getCurrentSubscription(UUID userId) {
        return userSubscriptionRepository.findByUser_Id(userId)
                .orElseThrow(() -> new UserSubscriptionNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public UserSubscription requireActiveSubscription(UUID userId) {
        var subscription = getCurrentSubscription(userId);
        if (!subscription.isPeriodActive(Instant.now(clock))) {
            throw new UserSubscriptionExpiredException(userId);
        }
        return subscription;
    }

    @Transactional(readOnly = true)
    public Plan getCurrentPlan(UUID userId) {
        return getCurrentSubscription(userId).getPlan();
    }

    @Transactional
    public void consumeCredits(
            UUID userId,
            UUID chatSessionId,
            UUID chatExchangeId,
            java.util.List<ModelUsage> usages
    ) {
        var now = Instant.now(clock);
        var subscription = requireActiveSubscription(userId);
        var usageRecords = usages.stream()
                .map(usage -> {
                    var charge = usagePricingService.calculateCharge(usage);
                    return UsageRecord.create(
                            userId,
                            chatSessionId,
                            chatExchangeId,
                            usage.stage(),
                            usage.modelUsed(),
                            usage.providerModelName(),
                            usage.inputTokens(),
                            usage.cachedInputTokens(),
                            usage.outputTokens(),
                            charge.costMicrosUsd(),
                            charge.pricingVersion(),
                            now
                    );
                })
                .toList();
        var totalCredits = usages.stream()
                .mapToLong(usage -> usageLimitProperties.creditCostFor(usage.stage()))
                .sum();

        subscription.consumeCredits(totalCredits, now);
        usageRecordRepository.saveAll(usageRecords);
        userSubscriptionRepository.save(subscription);
    }

    @Transactional
    public void renewSubscription(UUID userId, Plan plan) {
        var now = Instant.now(clock);
        var monthlyCreditLimit = usageLimitProperties.monthlyCreditLimitFor(plan);
        var periodEnd = now.plus(usageLimitProperties.periodDuration());
        var user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);

        var subscription = userSubscriptionRepository.findByUser_Id(userId)
                .orElseGet(() -> UserSubscription.create(
                        user,
                        plan,
                        monthlyCreditLimit,
                        now,
                        periodEnd,
                        now
                ));

        if (subscription.getId() != null) {
            subscription.renew(plan, monthlyCreditLimit, now, periodEnd, now);
        }

        userSubscriptionRepository.save(subscription);
    }

    @Transactional(readOnly = true)
    public UsageSummaryDTO getUsageSummary(UUID userId) {
        var subscription = getCurrentSubscription(userId);
        return userSubscriptionMapper.toUsageSummary(subscription);
    }
}
