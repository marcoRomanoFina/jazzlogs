create unique index if not exists uk_spotify_connections_connected_spotify_user_id
    on spotify_connections (spotify_user_id)
    where status = 'CONNECTED';
