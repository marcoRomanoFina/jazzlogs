alter table if exists album_logs
    rename to album_logs_legacy;

alter table if exists artist_profiles
    rename to artist_profiles_legacy;

alter table if exists track_notes
    rename to track_notes_legacy;

create table album_logs (
    id uuid primary key default gen_random_uuid(),
    log_number integer not null,
    album_name varchar(512) not null,
    main_artists jsonb not null default '[]'::jsonb,
    caption_essence text,
    posted_at date not null,
    instagram_permalink varchar(1024),
    style varchar(255),
    vocal_profile varchar(255),
    release_year varchar(32),
    moods jsonb not null default '[]'::jsonb,
    tier varchar(128),
    vibe jsonb not null default '[]'::jsonb,
    energy varchar(128),
    mood_intensity varchar(128),
    accessibility varchar(128),
    best_moment jsonb,
    listening_context jsonb not null default '[]'::jsonb,
    why_it_matters text,
    editorial_note text,
    recommended_if text,
    avoid_if text,
    album_context text,
    personnel jsonb not null default '[]'::jsonb,
    spotify_album_id varchar(64),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_album_logs_log_number unique (log_number),
    constraint uk_album_logs_spotify_album_id unique (spotify_album_id)
);

create table artist_logs (
    id uuid primary key default gen_random_uuid(),
    spotify_artist_id varchar(64) not null,
    artist_name varchar(512) not null,
    primary_instrument varchar(255),
    main_styles jsonb not null default '[]'::jsonb,
    sound_profile text,
    artist_context text,
    editorial_note text,
    entry_point_log_id varchar(255),
    best_listening_moments jsonb not null default '[]'::jsonb,
    avoid_if text,
    related_artists jsonb not null default '[]'::jsonb,
    why_it_matters text,
    appears_in_logs jsonb not null default '[]'::jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_artist_logs_spotify_artist_id unique (spotify_artist_id)
);

create table track_logs (
    id uuid primary key default gen_random_uuid(),
    spotify_track_id varchar(64) not null,
    spotify_album_id varchar(64),
    log_number integer,
    track_name varchar(512) not null,
    album_name varchar(512) not null,
    main_artist_spotify_id varchar(64),
    tier varchar(128),
    vocal_profile varchar(255),
    standout boolean,
    vibe jsonb not null default '[]'::jsonb,
    energy varchar(128),
    mood_intensity varchar(128),
    accessibility varchar(128),
    tempo_feel varchar(128),
    rhythm_feel varchar(128),
    album_role varchar(255),
    composition_type varchar(255),
    best_moment text,
    listening_context jsonb not null default '[]'::jsonb,
    why_it_hits text,
    editorial_note text,
    recommended_if text,
    avoid_if text,
    instrument_focus varchar(255),
    vocal_style varchar(255),
    standout_tags jsonb not null default '[]'::jsonb,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_track_logs_spotify_track_id unique (spotify_track_id)
);
