create table spotify_playlist_items (
    id uuid primary key default gen_random_uuid(),
    playlist_id varchar(64) not null,
    spotify_track_id varchar(64) not null,
    track_name varchar(512) not null,
    spotify_album_id varchar(64),
    album_name varchar(512),
    artist_names text not null,
    spotify_track_url varchar(512),
    spotify_album_url varchar(512),
    cover_image_url varchar(1024),
    added_at timestamp with time zone,
    duration_ms integer,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint uk_spotify_playlist_track unique (playlist_id, spotify_track_id)
);

create index idx_spotify_playlist_items_playlist_id on spotify_playlist_items (playlist_id);
create index idx_spotify_playlist_items_album_id on spotify_playlist_items (spotify_album_id);
