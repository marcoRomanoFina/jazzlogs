create table spotify_taste_snapshots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id),
    spotify_connection_id uuid not null references spotify_connections(id),
    time_range varchar(32) not null,
    top_artists jsonb not null,
    top_tracks jsonb not null,
    fetched_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create unique index uq_spotify_taste_snapshots_user_id_time_range
    on spotify_taste_snapshots (user_id, time_range);
