CREATE TABLE comunidade (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    municipio_id UUID NOT NULL,

    nome VARCHAR(120) NOT NULL,

    tipo VARCHAR(50) NOT NULL DEFAULT 'SITIO',

    lider_nome VARCHAR(120),

    lider_telefone VARCHAR(30),

    latitude NUMERIC(10,7),

    longitude NUMERIC(10,7),

    observacoes TEXT,

    CONSTRAINT fk_comunidade_municipio
        FOREIGN KEY (municipio_id)
        REFERENCES municipio(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_comunidade_municipio_id
    ON comunidade(municipio_id);