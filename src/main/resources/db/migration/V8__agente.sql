CREATE TABLE agente (
    id             uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    nome           varchar(120) NOT NULL,
    codigo_convite varchar(6)   UNIQUE,
    token_hash     varchar(255),
    ativo          boolean      NOT NULL DEFAULT true,
    ativado_em     timestamptz,
    criado_em      timestamptz  NOT NULL DEFAULT now()
);
