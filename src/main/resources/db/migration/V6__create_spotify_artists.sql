create table spotify_artists (
    spotify_artist_id varchar(64) primary key,
    name varchar(512) not null,
    spotify_url varchar(512),
    href varchar(512),
    uri varchar(128),
    type varchar(32),
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create table spotify_track_artists (
    id uuid primary key default gen_random_uuid(),
    spotify_track_id varchar(64) not null,
    spotify_artist_id varchar(64) not null,
    artist_order integer not null,
    created_at timestamp with time zone not null default current_timestamp,
    constraint fk_spotify_track_artists_track
        foreign key (spotify_track_id) references spotify_tracks (spotify_track_id)
        on delete cascade,
    constraint fk_spotify_track_artists_artist
        foreign key (spotify_artist_id) references spotify_artists (spotify_artist_id)
        on delete cascade,
    constraint uk_spotify_track_artist unique (spotify_track_id, spotify_artist_id)
);

create index idx_spotify_track_artists_track_id on spotify_track_artists (spotify_track_id);
create index idx_spotify_track_artists_artist_id on spotify_track_artists (spotify_artist_id);
