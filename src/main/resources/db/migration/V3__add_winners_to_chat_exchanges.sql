alter table chat_exchanges
    add column winners jsonb not null default '[]'::jsonb;
