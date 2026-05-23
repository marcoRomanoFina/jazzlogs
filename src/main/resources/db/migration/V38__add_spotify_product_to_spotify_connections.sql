alter table spotify_connections
    add column if not exists spotify_product varchar(64);
