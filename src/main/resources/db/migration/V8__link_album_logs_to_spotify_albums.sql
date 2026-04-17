alter table album_logs
    add column spotify_album_id varchar(64);

alter table album_logs
    add constraint fk_album_logs_spotify_album
        foreign key (spotify_album_id) references spotify_albums (spotify_album_id)
        on delete set null;

create unique index uk_album_logs_spotify_album_id
    on album_logs (spotify_album_id)
    where spotify_album_id is not null;
