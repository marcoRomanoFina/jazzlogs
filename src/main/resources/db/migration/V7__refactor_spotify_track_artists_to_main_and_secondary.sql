alter table spotify_tracks
    add column main_artist_id varchar(64);

alter table spotify_tracks
    add constraint fk_spotify_tracks_main_artist
        foreign key (main_artist_id) references spotify_artists (spotify_artist_id)
        on delete set null;

create index idx_spotify_tracks_main_artist_id on spotify_tracks (main_artist_id);

create table spotify_track_secondary_artists (
    spotify_track_id varchar(64) not null,
    spotify_artist_id varchar(64) not null,
    primary key (spotify_track_id, spotify_artist_id),
    constraint fk_spotify_track_secondary_artists_track
        foreign key (spotify_track_id) references spotify_tracks (spotify_track_id)
        on delete cascade,
    constraint fk_spotify_track_secondary_artists_artist
        foreign key (spotify_artist_id) references spotify_artists (spotify_artist_id)
        on delete cascade
);

update spotify_tracks st
set main_artist_id = sta.spotify_artist_id
from spotify_track_artists sta
where st.spotify_track_id = sta.spotify_track_id
  and sta.artist_order = 0;

insert into spotify_track_secondary_artists (spotify_track_id, spotify_artist_id)
select
    spotify_track_id,
    spotify_artist_id
from spotify_track_artists
where artist_order > 0
on conflict do nothing;

drop table spotify_track_artists;
