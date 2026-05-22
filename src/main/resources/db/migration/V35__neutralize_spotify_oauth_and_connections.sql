alter table spotify_connections
    add column if not exists spotify_display_name varchar(255);

alter table spotify_connections
    rename column scopes to granted_scopes;

alter table spotify_connections
    drop column if exists type;

drop index if exists idx_spotify_connections_type_status;

create index if not exists idx_spotify_connections_user_status
    on spotify_connections (user_id, status);

alter table spotify_oauth_states
    add column if not exists requested_scopes text[] not null default '{}';

alter table spotify_oauth_states
    drop column if exists flow_type;
