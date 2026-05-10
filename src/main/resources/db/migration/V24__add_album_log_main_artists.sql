alter table album_logs
    add column main_artists jsonb;

update album_logs
set main_artists = jsonb_build_array(
        jsonb_build_object(
                'spotifyArtistId', artist_id,
                'artistName', artist
        )
    )
where main_artists is null
  and artist is not null;

update album_logs
set main_artists = '[]'::jsonb
where main_artists is null;

alter table album_logs
    alter column main_artists set not null;
