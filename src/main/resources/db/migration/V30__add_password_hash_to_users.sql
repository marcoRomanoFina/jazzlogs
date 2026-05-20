alter table users
    add column if not exists password_hash varchar(255);

alter table users
    alter column beta_code_id drop not null;
