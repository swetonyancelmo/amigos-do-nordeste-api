-- Base de conhecimento do classificador de vulnerabilidade (ADR-0010).
-- Sistema especialista: o conhecimento (sentinelas, pesos, faixas, cortes e
-- rotulos) mora aqui, em dado; o codigo Java so sabe LER a familia e aplicar
-- o que esta nestas tabelas. Mudar um peso e um UPDATE, nao um deploy.
-- O conteudo (Escala de Coelho-Savassi) vem na V16; aqui so a estrutura.

-- Uma linha por sentinela do instrumento — inclusive as que nao sao
-- avaliadas, para que o corte fique registrado no proprio dado, com motivo.
CREATE TABLE vulnerabilidade_sentinela (
    codigo        VARCHAR(40)  PRIMARY KEY,
    -- nome como esta na Tabela 01 do artigo
    nome          VARCHAR(80)  NOT NULL,
    -- BINARIA: tem/nao tem, soma "pontos". FAIXA: mede uma razao e pontua
    -- conforme vulnerabilidade_faixa (relacao morador/comodo).
    tipo          VARCHAR(20)  NOT NULL,
    pontos        INTEGER,
    -- AVALIADA: entra no escore. NAO_COLETADA: o sistema nao tem o dado.
    -- DESCARTADA_LGPD: dado de saude, de proposito nao coletado.
    situacao      VARCHAR(20)  NOT NULL,
    justificativa TEXT,
    ordem         INTEGER      NOT NULL,

    CONSTRAINT chk_vuln_sentinela_tipo CHECK (tipo IN ('BINARIA', 'FAIXA')),
    CONSTRAINT chk_vuln_sentinela_situacao
        CHECK (situacao IN ('AVALIADA', 'NAO_COLETADA', 'DESCARTADA_LGPD')),
    CONSTRAINT chk_vuln_sentinela_pontos CHECK (
        (tipo = 'BINARIA' AND pontos IS NOT NULL AND pontos >= 0)
        OR (tipo = 'FAIXA' AND pontos IS NULL)
    )
);

-- Faixas de uma sentinela do tipo FAIXA: "razao <operador> limite -> pontos".
CREATE TABLE vulnerabilidade_faixa (
    sentinela_codigo VARCHAR(40)  NOT NULL,
    operador         VARCHAR(10)  NOT NULL,
    limite           NUMERIC(6,2) NOT NULL,
    pontos           INTEGER      NOT NULL,

    PRIMARY KEY (sentinela_codigo, operador, limite),
    CONSTRAINT fk_vuln_faixa_sentinela
        FOREIGN KEY (sentinela_codigo) REFERENCES vulnerabilidade_sentinela(codigo),
    CONSTRAINT chk_vuln_faixa_operador CHECK (operador IN ('MENOR', 'IGUAL', 'MAIOR')),
    CONSTRAINT chk_vuln_faixa_pontos CHECK (pontos >= 0)
);

-- Parametros de adaptacao de cada sentinela: quais valores dos enums contam
-- como saneamento precario, quais fontes de renda sao trabalho, limites de
-- idade. Um nome pode ter varios valores (um por linha).
CREATE TABLE vulnerabilidade_parametro (
    sentinela_codigo VARCHAR(40) NOT NULL,
    nome             VARCHAR(40) NOT NULL,
    valor            VARCHAR(40) NOT NULL,

    PRIMARY KEY (sentinela_codigo, nome, valor),
    CONSTRAINT fk_vuln_parametro_sentinela
        FOREIGN KEY (sentinela_codigo) REFERENCES vulnerabilidade_sentinela(codigo)
);

-- Estratos: o codigo e fiel ao instrumento (R1, R2, R3) para poder citar o
-- artigo; o rotulo e o que aparece na tela, e e configuravel (ADR-0010,
-- regra 4: texto estigmatiza). DADOS_INSUFICIENTES nao tem corte: e o motor
-- que o atribui quando falta dado para decidir.
CREATE TABLE vulnerabilidade_estrato (
    codigo                VARCHAR(30)  PRIMARY KEY,
    escore_minimo         INTEGER,
    rotulo                VARCHAR(80)  NOT NULL,
    descricao_instrumento VARCHAR(160) NOT NULL,
    -- ordem na lista "por prioridade" (1 primeiro)
    ordem                 INTEGER      NOT NULL,

    CONSTRAINT uq_vuln_estrato_ordem UNIQUE (ordem),
    CONSTRAINT chk_vuln_estrato_codigo
        CHECK (codigo IN ('R3', 'R2', 'R1', 'SEM_RISCO_IDENTIFICADO', 'DADOS_INSUFICIENTES')),
    CONSTRAINT chk_vuln_estrato_corte
        CHECK ((codigo = 'DADOS_INSUFICIENTES') = (escore_minimo IS NULL))
);
