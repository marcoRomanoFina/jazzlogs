alter table spotify_connections
    add column if not exists spotify_country varchar(16);
