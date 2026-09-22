CREATE TABLE municipio (
    id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome        varchar(120) NOT NULL,
    uf          char(2)      NOT NULL,
    codigo_ibge varchar(7)   UNIQUE,
    criado_em   timestamptz  NOT NULL DEFAULT now()
);

CREATE INDEX idx_municipio_nome ON municipio (nome);
