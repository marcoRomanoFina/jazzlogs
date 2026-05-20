create table users (
    id uuid primary key,
    beta_code varchar(64) not null,
    display_name varchar(128) not null,
    email varchar(320),
    spotify_user_id varchar(128),
    role varchar(32) not null,
    status varchar(32) not null,
    jazz_experience_level varchar(32),
    preferred_styles text[] not null default '{}'::text[],
    preferred_instruments text[] not null default '{}'::text[],
    preferred_moods text[] not null default '{}'::text[],
    preferred_tempo_feel varchar(32),
    vocal_preference varchar(32),
    created_at timestamptz not null default now(),
    updated_at timestamptz not null default now(),
    last_seen_at timestamptz
);

create unique index ux_users_beta_code on users (beta_code);
create unique index ux_users_email_lower on users (lower(email)) where email is not null;
create unique index ux_users_spotify_user_id on users (spotify_user_id) where spotify_user_id is not null;
