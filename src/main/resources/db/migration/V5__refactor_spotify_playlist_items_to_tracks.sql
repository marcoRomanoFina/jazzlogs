create table spotify_albums (
    spotify_album_id varchar(64) primary key,
    source_playlist_id varchar(64) not null,
    name varchar(512) not null,
    spotify_url varchar(512),
    cover_image_url varchar(1024),
    album_type varchar(32),
    total_tracks integer,
    release_date varchar(32),
    release_date_precision varchar(16),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_spotify_albums_source_playlist_id on spotify_albums (source_playlist_id);

create table spotify_tracks (
    spotify_track_id varchar(64) primary key,
    source_playlist_id varchar(64) not null,
    spotify_album_id varchar(64),
    name varchar(512) not null,
    artist_names text not null,
    spotify_url varchar(512),
    duration_ms integer,
    disc_number integer,
    track_number integer,
    added_to_playlist_at timestamp with time zone,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp,
    constraint fk_spotify_tracks_album
        foreign key (spotify_album_id) references spotify_albums (spotify_album_id)
        on delete set null
);

create index idx_spotify_tracks_source_playlist_id on spotify_tracks (source_playlist_id);
create index idx_spotify_tracks_album_id on spotify_tracks (spotify_album_id);

insert into spotify_albums (
    spotify_album_id,
    source_playlist_id,
    name,
    spotify_url,
    cover_image_url,
    created_at,
    updated_at
)
select distinct
    spotify_album_id,
    playlist_id,
    coalesce(album_name, ''),
    spotify_album_url,
    cover_image_url,
    created_at,
    updated_at
from spotify_playlist_items
where spotify_album_id is not null
on conflict (spotify_album_id) do nothing;

insert into spotify_tracks (
    spotify_track_id,
    source_playlist_id,
    spotify_album_id,
    name,
    artist_names,
    spotify_url,
    duration_ms,
    added_to_playlist_at,
    created_at,
    updated_at
)
select
    spotify_track_id,
    playlist_id,
    spotify_album_id,
    track_name,
    artist_names,
    spotify_track_url,
    duration_ms,
    added_at,
    created_at,
    updated_at
from spotify_playlist_items
on conflict (spotify_track_id) do nothing;

drop table spotify_playlist_items;
