drop table if exists spotify_taste_snapshots;

create table spotify_taste_snapshots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null references users(id),
    spotify_connection_id uuid not null references spotify_connections(id),
    top_artists jsonb not null,
    top_tracks jsonb not null,
    generated_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_spotify_taste_snapshots_user_generated_at
    on spotify_taste_snapshots (user_id, generated_at desc);

create index idx_spotify_taste_snapshots_connection_generated_at
    on spotify_taste_snapshots (spotify_connection_id, generated_at desc);
