CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE EXTENSION IF NOT EXISTS unaccent;

CREATE TABLE usuario (
    id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome             varchar(120) NOT NULL,
    email            varchar(160) NOT NULL UNIQUE,
    senha_hash       varchar(255) NOT NULL,
    ativo            boolean      NOT NULL DEFAULT true,
    ultimo_acesso_em timestamptz,
    criado_em        timestamptz  NOT NULL DEFAULT now()
);
