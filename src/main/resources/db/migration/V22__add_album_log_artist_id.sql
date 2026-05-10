alter table album_logs
    add column artist_id varchar(64);

create index idx_album_logs_artist_id on album_logs (artist_id);
