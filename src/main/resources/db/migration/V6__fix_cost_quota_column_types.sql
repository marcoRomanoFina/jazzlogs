alter table user_subscriptions
    alter column usage_limit_micros_usd type bigint using usage_limit_micros_usd::bigint;

alter table user_subscriptions
    alter column used_micros_usd type bigint using used_micros_usd::bigint;

alter table user_subscriptions
    alter column remaining_micros_usd type bigint using remaining_micros_usd::bigint;
