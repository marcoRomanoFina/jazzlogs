alter table spotify_connections
    add column if not exists user_id uuid references users(id),
    add column if not exists spotify_user_id varchar(128),
    add column if not exists type varchar(64) not null default 'ADMIN_PLAYLIST_SYNC',
    add column if not exists status varchar(32) not null default 'CONNECTED';

create index if not exists idx_spotify_connections_type_status
    on spotify_connections (type, status);

create table if not exists spotify_oauth_states (
    id uuid primary key default gen_random_uuid(),
    user_id uuid references users(id),
    state varchar(255) not null unique,
    status varchar(32) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    consumed_at timestamp with time zone,
    flow_type varchar(64) not null
);

create index if not exists idx_spotify_oauth_states_status_expires_at
    on spotify_oauth_states (status, expires_at);
