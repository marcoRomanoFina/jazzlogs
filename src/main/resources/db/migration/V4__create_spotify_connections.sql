create table spotify_connections (
    id uuid primary key default gen_random_uuid(),
    access_token text not null,
    refresh_token text not null,
    token_type varchar(32) not null,
    scopes text[] not null default '{}',
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);
