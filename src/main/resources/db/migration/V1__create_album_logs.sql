create table album_logs (
    id uuid primary key default gen_random_uuid(),
    log_number integer not null unique,
    album varchar(255) not null,
    artist varchar(255) not null,
    caption text not null,
    posted_at date not null,
    instagram_permalink varchar(512) not null unique,
    style varchar(255),
    moods text[] not null default '{}',
    notes text,
    created_at timestamp with time zone not null default current_timestamp,
    updated_at timestamp with time zone not null default current_timestamp
);

create index idx_album_logs_artist on album_logs (artist);
create index idx_album_logs_posted_at on album_logs (posted_at);
