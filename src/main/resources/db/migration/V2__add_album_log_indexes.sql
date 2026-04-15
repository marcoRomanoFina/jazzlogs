create index idx_album_logs_album on album_logs (album);

create index idx_album_logs_artist_album on album_logs (artist, album);
