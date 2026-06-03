package com.marcoromanofinaa.jazzlogs.user.subscription.model;

import com.marcoromanofinaa.jazzlogs.user.model.Plan;
import com.marcoromanofinaa.jazzlogs.user.model.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_subscriptions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_subscriptions_user_id", columnNames = "user_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false)
    private Plan plan;

    @Column(name = "usage_limit_micros_usd", nullable = false)
    private Long tokenLimit;

    @Column(name = "used_micros_usd", nullable = false)
    private Long tokensUsed;

    @Column(name = "remaining_micros_usd", nullable = false)
    private Long tokensRemaining;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static UserSubscription create(
            User user,
            Plan plan,
            Long tokenLimit,
            Instant periodStart,
            Instant periodEnd,
            Instant now
    ) {
        var subscription = new UserSubscription();
        subscription.user = user;
        subscription.plan = plan;
        subscription.tokenLimit = tokenLimit;
        subscription.tokensUsed = 0L;
        subscription.tokensRemaining = tokenLimit;
        subscription.periodStart = periodStart;
        subscription.periodEnd = periodEnd;
        subscription.createdAt = now;
        subscription.updatedAt = now;
        return subscription;
    }

    public UUID getUserId() {
        return user.getId();
    }

    public void renew(
            Plan plan,
            Long tokenLimit,
            Instant periodStart,
            Instant periodEnd,
            Instant now
    ) {
        this.plan = plan;
        this.tokenLimit = tokenLimit;
        this.tokensUsed = 0L;
        this.tokensRemaining = tokenLimit;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.updatedAt = now;
    }

    public void consumeTokens(Long consumedTokens, Instant now) {
        long safeConsumedTokens = Math.max(consumedTokens == null ? 0L : consumedTokens, 0L);
        this.tokensUsed = this.tokensUsed + safeConsumedTokens;
        this.tokensRemaining = Math.max(this.tokensRemaining - safeConsumedTokens, 0L);
        this.updatedAt = now;
    }

    public boolean isPeriodActive(Instant now) {
        return !now.isBefore(periodStart) && now.isBefore(periodEnd);
    }

    public double remainingPercentage() {
        if (tokenLimit == null || tokenLimit == 0) {
            return 0.0;
        }
        return (tokensRemaining.doubleValue() / tokenLimit.doubleValue()) * 100.0;
    }
}
