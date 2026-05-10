alter table track_notes
    add column main_artists jsonb;

update track_notes
set main_artists = jsonb_build_array(jsonb_build_object(
        'spotifyArtistId', coalesce(track_notes.artist_id, spotify_tracks.main_artist_id),
        'artistName', coalesce(spotify_artists.name, track_notes.artist_id)
    ))
from spotify_tracks
left join spotify_artists on spotify_artists.spotify_artist_id = spotify_tracks.main_artist_id
where main_artists is null
  and spotify_tracks.spotify_track_id = track_notes.spotify_track_id;

update track_notes
set main_artists = '[]'::jsonb
where main_artists is null;

alter table track_notes
    alter column main_artists set not null;
