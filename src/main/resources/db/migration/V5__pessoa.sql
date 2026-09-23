CREATE TABLE pessoa (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    familia_id UUID NOT NULL,

    nome VARCHAR(120),

    cadastro_incompleto BOOLEAN NOT NULL DEFAULT FALSE,

    sexo VARCHAR(30),

    data_nascimento DATE,

    idade_estimada INTEGER,

    idade_estimada_em DATE,

    parentesco VARCHAR(100),

    estuda BOOLEAN,

    serie VARCHAR(50),

    tamanho_roupa VARCHAR(20),

    numero_calcado VARCHAR(20),

    gestante BOOLEAN,

    observacoes TEXT,

    CONSTRAINT fk_pessoa_familia
        FOREIGN KEY (familia_id)
        REFERENCES familia(id)
        ON DELETE CASCADE,

    CONSTRAINT chk_pessoa_idade CHECK (
        data_nascimento IS NOT NULL
        OR (
            idade_estimada IS NOT NULL
            AND idade_estimada_em IS NOT NULL
        )
    )
);

CREATE INDEX idx_pessoa_familia_id
    ON pessoa(familia_id);