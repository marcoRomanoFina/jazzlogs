alter table if exists album_logs
    drop constraint if exists fk_album_logs_spotify_album;

alter table if exists spotify_track_secondary_artists
    rename to spotify_track_secondary_artists_legacy;

alter table if exists spotify_tracks
    rename to spotify_tracks_legacy;

alter table if exists spotify_albums
    rename to spotify_albums_legacy;

alter table if exists spotify_artists
    rename to spotify_artists_legacy;

create table spotify_artists (
    id uuid primary key default gen_random_uuid(),
    spotify_artist_id varchar(64) not null,
    name varchar(512) not null,
    spotify_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_artists_spotify_artist_id unique (spotify_artist_id)
);

create table spotify_albums (
    id uuid primary key default gen_random_uuid(),
    spotify_album_id varchar(64) not null,
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
    spotify_track_id varchar(64) not null,
    name varchar(512) not null,
    album_id uuid not null,
    duration_ms integer,
    track_number integer,
    spotify_url varchar(512),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint uk_spotify_tracks_spotify_track_id unique (spotify_track_id),
    constraint fk_spotify_tracks_album
        foreign key (album_id) references spotify_albums (id)
        on delete cascade
);

create table spotify_album_artists (
    id uuid primary key default gen_random_uuid(),
    album_id uuid not null,
    artist_id uuid not null,
    position integer not null,
    constraint fk_spotify_album_artists_album
        foreign key (album_id) references spotify_albums (id)
        on delete cascade,
    constraint fk_spotify_album_artists_artist
        foreign key (artist_id) references spotify_artists (id)
        on delete cascade,
    constraint uk_spotify_album_artists_album_artist unique (album_id, artist_id),
    constraint uk_spotify_album_artists_album_position unique (album_id, position)
);

create table spotify_track_artists (
    id uuid primary key default gen_random_uuid(),
    track_id uuid not null,
    artist_id uuid not null,
    position integer not null,
    constraint fk_spotify_track_artists_track
        foreign key (track_id) references spotify_tracks (id)
        on delete cascade,
    constraint fk_spotify_track_artists_artist
        foreign key (artist_id) references spotify_artists (id)
        on delete cascade,
    constraint uk_spotify_track_artists_track_artist unique (track_id, artist_id),
    constraint uk_spotify_track_artists_track_position unique (track_id, position)
);

create index idx_spotify_album_artists_album_id
    on spotify_album_artists (album_id);

create index idx_spotify_album_artists_artist_id
    on spotify_album_artists (artist_id);

create index idx_spotify_track_artists_track_id
    on spotify_track_artists (track_id);

create index idx_spotify_track_artists_artist_id
    on spotify_track_artists (artist_id);

insert into spotify_artists (
    spotify_artist_id,
    name,
    spotify_url,
    created_at,
    updated_at
)
select
    legacy.spotify_artist_id,
    legacy.name,
    legacy.spotify_url,
    legacy.created_at,
    legacy.updated_at
from spotify_artists_legacy legacy;

insert into spotify_albums (
    spotify_album_id,
    name,
    release_date,
    total_tracks,
    image_url,
    spotify_url,
    created_at,
    updated_at
)
select
    legacy.spotify_album_id,
    legacy.name,
    legacy.release_date,
    legacy.total_tracks,
    legacy.cover_image_url,
    legacy.spotify_url,
    legacy.created_at,
    legacy.updated_at
from spotify_albums_legacy legacy;

insert into spotify_tracks (
    spotify_track_id,
    name,
    album_id,
    duration_ms,
    track_number,
    spotify_url,
    created_at,
    updated_at
)
select
    legacy.spotify_track_id,
    legacy.name,
    albums.id,
    legacy.duration_ms,
    legacy.track_number,
    legacy.spotify_url,
    legacy.created_at,
    legacy.updated_at
from spotify_tracks_legacy legacy
join spotify_albums albums
    on albums.spotify_album_id = legacy.spotify_album_id;

insert into spotify_track_artists (
    track_id,
    artist_id,
    position
)
select
    tracks.id,
    artists.id,
    0
from spotify_tracks_legacy legacy
join spotify_tracks tracks
    on tracks.spotify_track_id = legacy.spotify_track_id
join spotify_artists artists
    on artists.spotify_artist_id = legacy.main_artist_id
where legacy.main_artist_id is not null;

insert into spotify_track_artists (
    track_id,
    artist_id,
    position
)
select
    tracks.id,
    artists.id,
    row_number() over (
        partition by legacy.spotify_track_id
        order by artists.spotify_artist_id
    )
from spotify_track_secondary_artists_legacy legacy
join spotify_tracks tracks
    on tracks.spotify_track_id = legacy.spotify_track_id
join spotify_artists artists
    on artists.spotify_artist_id = legacy.spotify_artist_id
on conflict (track_id, artist_id) do nothing;

with ordered_album_artists as (
    select
        tracks.album_id,
        track_artists.artist_id,
        min(track_artists.position) as first_position
    from spotify_track_artists track_artists
    join spotify_tracks tracks
        on tracks.id = track_artists.track_id
    group by tracks.album_id, track_artists.artist_id
),
ranked_album_artists as (
    select
        album_id,
        artist_id,
        row_number() over (
            partition by album_id
            order by first_position, artist_id
        ) - 1 as position
    from ordered_album_artists
)
insert into spotify_album_artists (
    album_id,
    artist_id,
    position
)
select
    album_id,
    artist_id,
    position
from ranked_album_artists;

alter table album_logs
    add constraint fk_album_logs_spotify_album
        foreign key (spotify_album_id) references spotify_albums (spotify_album_id)
        on delete set null;

drop table if exists spotify_track_secondary_artists_legacy;
drop table if exists spotify_tracks_legacy;
drop table if exists spotify_albums_legacy;
drop table if exists spotify_artists_legacy;
