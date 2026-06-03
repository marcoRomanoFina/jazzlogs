alter table user_subscriptions
    rename column token_limit to usage_limit_micros_usd;

alter table user_subscriptions
    rename column tokens_used to used_micros_usd;

alter table user_subscriptions
    rename column tokens_remaining to remaining_micros_usd;

update user_subscriptions
set usage_limit_micros_usd = case plan
        when 'FREE' then 500000
        when 'TRIAL' then 1000000
        when 'PLUS' then 5000000
        when 'PRO' then 15000000
        else 500000
    end,
    used_micros_usd = 0,
    remaining_micros_usd = case plan
        when 'FREE' then 500000
        when 'TRIAL' then 1000000
        when 'PLUS' then 5000000
        when 'PRO' then 15000000
        else 500000
    end;

alter table usage_records
    add column cost_micros_usd bigint not null default 0;

alter table usage_records
    add column pricing_version varchar(64) not null default '2026-05-28';
