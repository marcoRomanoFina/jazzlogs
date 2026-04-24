drop index if exists idx_album_logs_artist;

drop index if exists idx_album_logs_posted_at;

drop index if exists idx_album_logs_album;

drop index if exists idx_album_logs_artist_album;

create index idx_album_logs_unlinked_log_number
    on album_logs (log_number)
    where spotify_album_id is null;
