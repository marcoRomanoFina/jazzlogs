update spotify_taste_snapshots
set top_artists = case
    when jsonb_typeof(top_artists) = 'array' then top_artists
    when jsonb_typeof(top_artists) = 'object' then coalesce(
        top_artists -> 'MEDIUM_TERM',
        top_artists -> 'LONG_TERM',
        top_artists -> 'SHORT_TERM',
        (
            select value
            from jsonb_each(top_artists)
            limit 1
        ),
        '[]'::jsonb
    )
    else '[]'::jsonb
end,
top_tracks = case
    when jsonb_typeof(top_tracks) = 'array' then top_tracks
    when jsonb_typeof(top_tracks) = 'object' then coalesce(
        top_tracks -> 'MEDIUM_TERM',
        top_tracks -> 'LONG_TERM',
        top_tracks -> 'SHORT_TERM',
        (
            select value
            from jsonb_each(top_tracks)
            limit 1
        ),
        '[]'::jsonb
    )
    else '[]'::jsonb
end;
