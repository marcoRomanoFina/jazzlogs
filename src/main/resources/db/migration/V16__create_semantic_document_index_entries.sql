create table semantic_document_index_entries (
    id uuid primary key,
    document_id varchar(160) not null,
    source_type varchar(64) not null,
    source_id varchar(128) not null,
    embedding_text_hash varchar(64) not null,
    transformer_version varchar(64) not null,
    indexed_at timestamptz not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_semantic_document_index_document_id unique (document_id)
);

create index idx_semantic_document_index_source
    on semantic_document_index_entries (source_type, source_id);
