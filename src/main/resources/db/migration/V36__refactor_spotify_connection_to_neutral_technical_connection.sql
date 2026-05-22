alter table spotify_connections
    rename column access_token to encrypted_access_token;

alter table spotify_connections
    rename column refresh_token to encrypted_refresh_token;

alter table spotify_connections
    alter column user_id set not null;

alter table spotify_connections
    alter column spotify_user_id set not null;

alter table spotify_connections
    alter column granted_scopes type text using array_to_string(granted_scopes, ' ');

alter table spotify_connections
    alter column granted_scopes set default '';

alter table spotify_connections
    add column if not exists connected_at timestamp with time zone not null default current_timestamp,
    add column if not exists last_refreshed_at timestamp with time zone,
    add column if not exists last_used_at timestamp with time zone,
    add column if not exists disconnected_at timestamp with time zone;

alter table spotify_connections
    add constraint uk_spotify_connections_user_id unique (user_id);
