alter table usage_records
    add column cached_input_tokens integer not null default 0;
