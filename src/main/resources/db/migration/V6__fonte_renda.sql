CREATE TABLE fonte_renda (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    familia_id UUID NOT NULL,

    pessoa_id UUID,

    tipo VARCHAR(100) NOT NULL,

    faixa VARCHAR(100),

    observacao TEXT,

    CONSTRAINT fk_fonte_renda_familia
        FOREIGN KEY (familia_id)
        REFERENCES familia(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_fonte_renda_pessoa
        FOREIGN KEY (pessoa_id)
        REFERENCES pessoa(id)
        ON DELETE SET NULL
);

CREATE INDEX idx_fonte_renda_familia_id
    ON fonte_renda(familia_id);