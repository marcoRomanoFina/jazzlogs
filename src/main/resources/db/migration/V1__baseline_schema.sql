create extension if not exists pgcrypto;
create extension if not exists vector;

create table users (
    id uuid primary key default gen_random_uuid(),
    first_name varchar(120) not null,
    last_name varchar(120) not null,
    display_name varchar(120) not null,
    email varchar(320),
    password_hash varchar(255),
    role varchar(30) not null,
    status varchar(30) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    last_login_at timestamp with time zone
);

create unique index uk_users_email_lower
    on users (lower(email))
    where email is not null;

create table user_subscriptions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    plan varchar(32) not null,
    token_limit integer not null,
    tokens_used integer not null,
    tokens_remaining integer not null,
    period_start timestamp with time zone not null,
    period_end timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_user_subscriptions_user_id unique (user_id),
    constraint fk_user_subscriptions_user_id
        foreign key (user_id) references users (id)
);

create table user_jazz_preferences (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    jazz_experience_level varchar(40) not null,
    favorite_artists jsonb not null default '[]'::jsonb,
    preferred_subgenres jsonb not null default '[]'::jsonb,
    preferred_moods jsonb not null default '[]'::jsonb,
    favorite_instruments jsonb not null default '[]'::jsonb,
    tempo_feel varchar(20) not null,
    likes_vocals boolean not null,
    discovery_mode varchar(40) not null,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_user_jazz_preferences_user_id unique (user_id),
    constraint fk_user_jazz_preferences_user_id
        foreign key (user_id) references users (id)
);

create table spotify_oauth_states (
    id uuid primary key default gen_random_uuid(),
    user_id uuid,
    state varchar(255) not null,
    status varchar(32) not null,
    expires_at timestamp with time zone not null,
    created_at timestamp with time zone not null default current_timestamp,
    consumed_at timestamp with time zone,
    requested_scopes text[] not null,
    constraint uk_spotify_oauth_states_state unique (state),
    constraint fk_spotify_oauth_states_user_id
        foreign key (user_id) references users (id)
);

create index idx_spotify_oauth_states_status_expires_at
    on spotify_oauth_states (status, expires_at);

create index idx_spotify_oauth_states_status_created_at
    on spotify_oauth_states (status, created_at);

create table spotify_connections (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    spotify_user_id varchar(255) not null,
    spotify_display_name varchar(255),
    spotify_country varchar(16),
    spotify_product varchar(64),
    encrypted_access_token text not null,
    encrypted_refresh_token text not null,
    token_type varchar(255) not null,
    granted_scopes text not null,
    expires_at timestamp with time zone not null,
    status varchar(32) not null,
    connected_at timestamp with time zone not null,
    last_refreshed_at timestamp with time zone,
    last_used_at timestamp with time zone,
    disconnected_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_connections_user_id unique (user_id),
    constraint fk_spotify_connections_user_id
        foreign key (user_id) references users (id)
);

create index idx_spotify_connections_user_status
    on spotify_connections (user_id, status);

create index idx_spotify_connections_spotify_user_status
    on spotify_connections (spotify_user_id, status);

create unique index uk_spotify_connections_connected_spotify_user_id
    on spotify_connections (spotify_user_id)
    where status = 'CONNECTED';

create table spotify_taste_snapshots (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    spotify_connection_id uuid not null,
    top_artists jsonb not null default '[]'::jsonb,
    top_tracks jsonb not null default '[]'::jsonb,
    generated_at timestamp with time zone not null,
    constraint fk_spotify_taste_snapshots_user_id
        foreign key (user_id) references users (id),
    constraint fk_spotify_taste_snapshots_spotify_connection_id
        foreign key (spotify_connection_id) references spotify_connections (id)
);

create index idx_spotify_taste_snapshots_user_generated_at
    on spotify_taste_snapshots (user_id, generated_at desc);

create index idx_spotify_taste_snapshots_connection_generated_at
    on spotify_taste_snapshots (spotify_connection_id, generated_at desc);

create table spotify_artists (
    id uuid primary key default gen_random_uuid(),
    spotify_artist_id varchar(255) not null,
    name varchar(512) not null,
    spotify_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_artists_spotify_artist_id unique (spotify_artist_id)
);

create table spotify_albums (
    id uuid primary key default gen_random_uuid(),
    spotify_album_id varchar(255) not null,
    name varchar(512) not null,
    release_date varchar(32),
    total_tracks integer,
    image_url varchar(1024),
    spotify_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_albums_spotify_album_id unique (spotify_album_id)
);

create table spotify_tracks (
    id uuid primary key default gen_random_uuid(),
    spotify_track_id varchar(255) not null,
    name varchar(512) not null,
    album_id uuid not null,
    duration_ms integer,
    track_number integer,
    spotify_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_tracks_spotify_track_id unique (spotify_track_id),
    constraint fk_spotify_tracks_album_id
        foreign key (album_id) references spotify_albums (id)
);

create index idx_spotify_tracks_album_id
    on spotify_tracks (album_id);

create table spotify_album_artists (
    album_id uuid not null,
    artist_id uuid not null,
    position integer not null,
    primary key (album_id, position),
    constraint fk_spotify_album_artists_album_id
        foreign key (album_id) references spotify_albums (id),
    constraint fk_spotify_album_artists_artist_id
        foreign key (artist_id) references spotify_artists (id),
    constraint uk_spotify_album_artists_album_artist unique (album_id, artist_id)
);

create index idx_spotify_album_artists_artist_id
    on spotify_album_artists (artist_id);

create table spotify_track_artists (
    track_id uuid not null,
    artist_id uuid not null,
    position integer not null,
    primary key (track_id, position),
    constraint fk_spotify_track_artists_track_id
        foreign key (track_id) references spotify_tracks (id),
    constraint fk_spotify_track_artists_artist_id
        foreign key (artist_id) references spotify_artists (id),
    constraint uk_spotify_track_artists_track_artist unique (track_id, artist_id)
);

create index idx_spotify_track_artists_artist_id
    on spotify_track_artists (artist_id);

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
    spotify_album_id varchar(255),
    indexing_status varchar(32) not null,
    indexed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_album_logs_log_number unique (log_number),
    constraint uk_album_logs_spotify_album_id unique (spotify_album_id)
);

create index idx_album_logs_indexing_status
    on album_logs (indexing_status);

create table artist_logs (
    id uuid primary key default gen_random_uuid(),
    spotify_artist_id varchar(255) not null,
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
    indexing_status varchar(32) not null,
    indexed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_artist_logs_spotify_artist_id unique (spotify_artist_id)
);

create index idx_artist_logs_indexing_status
    on artist_logs (indexing_status);

create table track_logs (
    id uuid primary key default gen_random_uuid(),
    spotify_track_id varchar(255) not null,
    spotify_album_id varchar(255),
    log_number integer,
    track_name varchar(512) not null,
    album_name varchar(512) not null,
    main_artist_spotify_id varchar(255),
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
    indexing_status varchar(32) not null,
    indexed_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_track_logs_spotify_track_id unique (spotify_track_id)
);

create index idx_track_logs_indexing_status
    on track_logs (indexing_status);

create table outbox_events (
    id uuid primary key default gen_random_uuid(),
    type varchar(128) not null,
    payload text not null,
    status varchar(32) not null,
    retry_count integer not null,
    next_retry_at timestamp with time zone,
    created_at timestamp with time zone not null,
    last_attempt_at timestamp with time zone,
    processed_at timestamp with time zone
);

create index idx_outbox_events_pending
    on outbox_events (status, created_at, next_retry_at);

create table chat_sessions (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    title varchar(255),
    last_interaction_at timestamp with time zone not null,
    deleted_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_chat_sessions_user_id
        foreign key (user_id) references users (id)
);

create index idx_chat_sessions_user_last_interaction
    on chat_sessions (user_id, last_interaction_at desc);

create table chat_exchanges (
    id uuid primary key default gen_random_uuid(),
    chat_session_id uuid not null,
    user_message text not null,
    requested_model varchar(32) not null,
    assistant_response text not null,
    recommendation_type varchar(32),
    model_used varchar(32) not null,
    created_at timestamp with time zone not null,
    constraint fk_chat_exchanges_chat_session_id
        foreign key (chat_session_id) references chat_sessions (id)
);

create index idx_chat_exchanges_session_created_at
    on chat_exchanges (chat_session_id, created_at);

create table usage_records (
    id uuid primary key default gen_random_uuid(),
    user_id uuid not null,
    chat_session_id uuid not null,
    chat_exchange_id uuid not null,
    model_used varchar(32) not null,
    input_tokens integer not null,
    output_tokens integer not null,
    total_tokens integer not null,
    created_at timestamp with time zone not null,
    constraint fk_usage_records_user_id
        foreign key (user_id) references users (id),
    constraint fk_usage_records_chat_session_id
        foreign key (chat_session_id) references chat_sessions (id),
    constraint fk_usage_records_chat_exchange_id
        foreign key (chat_exchange_id) references chat_exchanges (id)
);

create index idx_usage_records_user_created_at
    on usage_records (user_id, created_at);
