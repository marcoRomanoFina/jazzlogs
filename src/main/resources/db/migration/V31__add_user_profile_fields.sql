alter table users
    add column if not exists first_name varchar(120),
    add column if not exists last_name varchar(120),
    add column if not exists plan varchar(30) not null default 'FREE',
    add column if not exists jazz_preferences jsonb;

update users
set first_name = coalesce(first_name, display_name),
    last_name = coalesce(last_name, '')
where first_name is null or last_name is null;

alter table users
    alter column first_name set not null,
    alter column last_name set not null;
