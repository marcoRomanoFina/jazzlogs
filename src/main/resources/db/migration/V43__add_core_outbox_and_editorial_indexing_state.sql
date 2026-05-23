create table outbox_events (
    id uuid primary key default gen_random_uuid(),
    type varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    retry_count integer not null default 0,
    next_retry_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    last_attempt_at timestamp with time zone,
    processed_at timestamp with time zone
);

create index idx_outbox_events_pending
    on outbox_events (status, created_at, next_retry_at);

alter table album_logs
    add column if not exists indexing_status varchar(32) not null default 'PENDING',
    add column if not exists indexed_at timestamp with time zone;

alter table artist_logs
    add column if not exists indexing_status varchar(32) not null default 'PENDING',
    add column if not exists indexed_at timestamp with time zone;

alter table track_logs
    add column if not exists indexing_status varchar(32) not null default 'PENDING',
    add column if not exists indexed_at timestamp with time zone;
