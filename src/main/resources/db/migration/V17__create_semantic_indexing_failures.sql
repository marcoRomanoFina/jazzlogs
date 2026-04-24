create table semantic_indexing_failures (
    id uuid primary key,
    type varchar(32) not null,
    source_identifier varchar(128) not null,
    failure_type varchar(256) not null,
    failure_message text,
    failed_attempts integer not null,
    first_failed_at timestamptz not null,
    last_failed_at timestamptz not null,
    constraint uk_semantic_indexing_failures_type_source unique (type, source_identifier)
);

create index idx_semantic_indexing_failures_last_failed_at
    on semantic_indexing_failures (last_failed_at desc);
