alter table usage_records
    add column stage varchar(64) not null default 'BASIC_RECOMMENDATION',
    add column provider_model_name varchar(128) not null default 'unknown';
