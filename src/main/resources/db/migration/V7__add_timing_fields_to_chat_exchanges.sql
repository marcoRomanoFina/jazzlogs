alter table chat_exchanges
    add column router_latency_ms bigint not null default 0;

alter table chat_exchanges
    add column flow_latency_ms bigint not null default 0;

alter table chat_exchanges
    add column total_recommendation_latency_ms bigint not null default 0;
