create table track_notes (
    id uuid primary key default gen_random_uuid(),
    spotify_track_id varchar(64) not null unique,
    spotify_album_id varchar(64),
    log_number integer not null,
    track varchar(512) not null,
    album varchar(512) not null,
    artist_id varchar(64) not null,
    tier varchar(64),
    is_instrumental boolean not null,
    is_standout boolean not null,
    vibe text[] not null default '{}',
    energy varchar(32),
    mood_intensity varchar(32),
    accessibility varchar(32),
    tempo_feel varchar(32),
    rhythmic_feel varchar(64),
    track_role varchar(64),
    composition_type varchar(64),
    best_moment text,
    listening_context text[] not null default '{}',
    why_it_hits text,
    editorial_note text,
    recommended_if text,
    avoid_if text,
    instrument_focus varchar(128),
    vocal_style varchar(128),
    standout_tags text[] not null default '{}',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_track_notes_log_number on track_notes (log_number);
create index idx_track_notes_spotify_album_id on track_notes (spotify_album_id);
create index idx_track_notes_artist_id on track_notes (artist_id);

create table artist_profiles (
    id uuid primary key default gen_random_uuid(),
    spotify_artist_id varchar(64) not null unique,
    name varchar(512) not null,
    primary_instrument varchar(128),
    main_styles text[] not null default '{}',
    signature_sound text,
    artist_context text,
    jazzlogs_take text,
    recommended_entry_point varchar(512),
    best_for text[] not null default '{}',
    avoid_if text,
    related_artists text[] not null default '{}',
    importance text,
    log_appearances integer[] not null default '{}',
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_artist_profiles_name on artist_profiles (name);
