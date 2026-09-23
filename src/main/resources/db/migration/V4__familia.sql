CREATE TABLE familia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    comunidade_id UUID NOT NULL,

    responsavel_nome VARCHAR(120) NOT NULL,

    responsavel_cpf VARCHAR(14),

    telefone VARCHAR(30),

    ponto_referencia VARCHAR(255),

    banheiro VARCHAR(100),

    escoamento_sanitario VARCHAR(100),

    tratamento_agua VARCHAR(100),

    observacoes TEXT,

    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_familia_comunidade
        FOREIGN KEY (comunidade_id)
        REFERENCES comunidade(id)
        ON DELETE CASCADE
);

CREATE INDEX idx_familia_comunidade_id
    ON familia(comunidade_id);

CREATE INDEX idx_familia_responsavel_nome
    ON familia(responsavel_nome);


CREATE TABLE familia_abastecimento_agua (
    familia_id UUID NOT NULL,

    abastecimento VARCHAR(30) NOT NULL,

    PRIMARY KEY (familia_id, abastecimento),

    CONSTRAINT fk_familia_abastecimento_agua
        FOREIGN KEY (familia_id)
        REFERENCES familia(id)
        ON DELETE CASCADE
);