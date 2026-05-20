create table beta_invite_codes (
    id uuid primary key default gen_random_uuid(),
    code varchar(120) not null,
    max_uses integer not null default 1,
    used_count integer not null default 0,
    status varchar(30) not null,
    expires_at timestamptz,
    created_by varchar(120),
    note text,
    created_at timestamptz not null default now()
);

create unique index ux_beta_invite_codes_code on beta_invite_codes (code);

insert into beta_invite_codes (code, max_uses, used_count, status, created_at)
select upper(u.beta_code), 1, 1, 'ACTIVE', now()
from users u
on conflict (code) do nothing;

alter table users
    add column beta_code_id uuid;

update users u
set beta_code_id = b.id
from beta_invite_codes b
where b.code = upper(u.beta_code);

alter table users
    rename column last_seen_at to last_login_at;

alter table users
    alter column display_name type varchar(120),
    alter column role type varchar(30),
    alter column status type varchar(30);

alter table users
    alter column beta_code_id set not null;

alter table users
    add constraint fk_users_beta_code_id
        foreign key (beta_code_id) references beta_invite_codes(id);

create unique index ux_users_beta_code_id on users (beta_code_id);

drop index ux_users_beta_code;

alter table users
    drop column beta_code;
