create table if not exists user_jazz_preferences (
    id uuid primary key,
    user_id uuid not null unique references users(id),
    jazz_experience_level varchar(40) not null,
    favorite_artists jsonb not null,
    preferred_subgenres jsonb not null,
    preferred_moods jsonb not null,
    favorite_instruments jsonb not null,
    tempo_feel varchar(20) not null,
    likes_vocals boolean not null,
    discovery_mode varchar(40) not null,
    created_at timestamp not null,
    updated_at timestamp not null
);
