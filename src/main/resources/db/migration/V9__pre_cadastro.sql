CREATE TABLE pre_cadastro (
    id               uuid PRIMARY KEY,
    agente_id        uuid NOT NULL REFERENCES agente(id),
    comunidade_id    uuid REFERENCES comunidade(id),
    situacao         varchar(20) NOT NULL,   -- PENDENTE | APROVADO | DEVOLVIDO
    payload          jsonb NOT NULL,
    familia_id       uuid REFERENCES familia(id),
    motivo_devolucao text,
    recebido_em      timestamptz NOT NULL DEFAULT now(),
    avaliado_em      timestamptz
);

CREATE INDEX idx_pre_situacao ON pre_cadastro (situacao);
