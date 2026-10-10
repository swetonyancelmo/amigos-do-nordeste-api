-- Conteudo da base de conhecimento: Escala de Risco Familiar de
-- Coelho-Savassi (Rev Bras Med Fam Comunidade 2004;1(2):19-26,
-- doi:10.5712/rbmfc1(2)104), com a adaptacao registrada na ADR-0010.
--
-- Nomes e pontos sao os da Tabela 01 do artigo; cortes, os do Quadro 02.
-- NAO "ajuste" pontos ou cortes aqui para compensar sentinela que falta:
-- isso cria um instrumento novo, sem validacao. O teste
-- BaseDeConhecimentoSementeTest confere este arquivo contra o artigo.
--
-- Este arquivo tambem e a semente dos testes (H2, application-test.yml):
-- mantenha SQL portavel, so INSERT.

-- ---------------------------------------------------------------- avaliadas
INSERT INTO vulnerabilidade_sentinela (codigo, nome, tipo, pontos, situacao, justificativa, ordem) VALUES
('BAIXAS_CONDICOES_SANEAMENTO', 'Baixas condições de saneamento', 'BINARIA', 3, 'AVALIADA',
 'Presente se: sem banheiro; ou escoamento a céu aberto ou direto em rio/lago/mar; ou água de beber sem tratamento; ou água só de carro-pipa ou captação direta de rio. Critério da ADR-0010.', 1),
('DESEMPREGO', 'Desemprego', 'BINARIA', 2, 'AVALIADA',
 'Presente se a família não tem nenhuma fonte de renda de trabalho (fixo, informal ou sazonal) e tem ao menos uma pessoa de 18 a 59 anos. Proxy declarado na ADR-0010.', 2),
('MENOR_DE_SEIS_MESES', 'Menor de seis meses', 'BINARIA', 1, 'AVALIADA',
 'Só detectável com data de nascimento. Pessoa só com idade estimada deixa a sentinela indeterminada.', 3),
('MAIOR_DE_70_ANOS', 'Maior de 70 anos', 'BINARIA', 1, 'AVALIADA',
 'Idade em anos completos igual ou maior que 70: quem completou 70 já viveu mais de 70 anos.', 4),
('RELACAO_MORADOR_COMODO', 'Relação morador/cômodo', 'FAIXA', NULL, 'AVALIADA',
 'Pessoas da família dividido por familia.numero_comodos (V17).', 5);

-- ------------------------------------------------------------ nao avaliadas
INSERT INTO vulnerabilidade_sentinela (codigo, nome, tipo, pontos, situacao, justificativa, ordem) VALUES
('ANALFABETISMO', 'Analfabetismo', 'BINARIA', 1, 'NAO_COLETADA',
 'O cadastro não pergunta se a pessoa sabe ler e escrever. "Estuda" não é alfabetização e não é usado como substituto.', 6),
('ACAMADO', 'Acamado', 'BINARIA', 3, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 7),
('DEFICIENCIA_FISICA', 'Deficiência Física', 'BINARIA', 3, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 8),
('DEFICIENCIA_MENTAL', 'Deficiência mental', 'BINARIA', 3, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 9),
('DESNUTRICAO_GRAVE', 'Desnutrição (Grave)', 'BINARIA', 3, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 10),
('DROGADICAO', 'Drogadição', 'BINARIA', 2, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 11),
('HIPERTENSAO_ARTERIAL', 'Hipertensão Arterial Sistêmica', 'BINARIA', 1, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 12),
('DIABETES_MELLITUS', 'Diabetes Mellitus', 'BINARIA', 1, 'DESCARTADA_LGPD',
 'Dado de saúde (LGPD, art. 5º, II e art. 11). A associação não coleta nem guarda.', 13);

-- ------------------------------------------------- relacao morador/comodo
-- Tabela 01: "Se maior que 1" = 3, "Se igual a 1" = 2, "Se menor que 1" = 0.
INSERT INTO vulnerabilidade_faixa (sentinela_codigo, operador, limite, pontos) VALUES
('RELACAO_MORADOR_COMODO', 'MAIOR', 1.00, 3),
('RELACAO_MORADOR_COMODO', 'IGUAL', 1.00, 2),
('RELACAO_MORADOR_COMODO', 'MENOR', 1.00, 0);

-- ----------------------------------------------- parametros de adaptacao
-- Valores sao o name() dos enums do cadastro (categorias do e-SUS).
-- Abastecimento por cisterna NAO entra: no sertao e a regra, nao a excecao,
-- e o proprio artigo (secao 2.4) manda tratar o que atinge a comunidade
-- inteira como risco da area, nao da familia.
INSERT INTO vulnerabilidade_parametro (sentinela_codigo, nome, valor) VALUES
('BAIXAS_CONDICOES_SANEAMENTO', 'ESCOAMENTO_PRECARIO', 'CEU_ABERTO'),
('BAIXAS_CONDICOES_SANEAMENTO', 'ESCOAMENTO_PRECARIO', 'DIRETO_RIO_LAGO_MAR'),
('BAIXAS_CONDICOES_SANEAMENTO', 'TRATAMENTO_PRECARIO', 'SEM_TRATAMENTO'),
('BAIXAS_CONDICOES_SANEAMENTO', 'ABASTECIMENTO_SO_DE', 'CARRO_PIPA'),
('BAIXAS_CONDICOES_SANEAMENTO', 'ABASTECIMENTO_SO_DE', 'CAPTACAO_DIRETA_RIO'),
('DESEMPREGO', 'FONTE_DE_TRABALHO', 'TRABALHO_FIXO'),
('DESEMPREGO', 'FONTE_DE_TRABALHO', 'TRABALHO_INFORMAL'),
('DESEMPREGO', 'FONTE_DE_TRABALHO', 'TRABALHO_SAZONAL'),
('DESEMPREGO', 'IDADE_ATIVA_MINIMA', '18'),
('DESEMPREGO', 'IDADE_ATIVA_MAXIMA', '59'),
('MENOR_DE_SEIS_MESES', 'MESES', '6'),
('MAIOR_DE_70_ANOS', 'IDADE_MINIMA', '70');

-- ----------------------------------------------------------------- estratos
-- Quadro 02: 5 ou 6 = R1; 7 ou 8 = R2; 9 ou mais = R3 (o quadro escreve
-- "Maior que 9", mas a Tabela 2 e o texto do artigo usam "9 ou maior").
-- Abaixo de 5 o artigo nao classifica: estrato proprio, sem codigo R.
INSERT INTO vulnerabilidade_estrato (codigo, escore_minimo, rotulo, descricao_instrumento, ordem) VALUES
('R3', 9, 'Maior necessidade de apoio', 'R3 — risco máximo (escore 9 ou mais)', 1),
('R2', 7, 'Necessidade intermediária de apoio', 'R2 — risco médio (escore 7 ou 8)', 2),
('R1', 5, 'Necessidade de apoio a acompanhar', 'R1 — risco menor (escore 5 ou 6)', 3),
('DADOS_INSUFICIENTES', NULL, 'Completar cadastro para avaliar', 'Fora do instrumento: falta dado para decidir o estrato', 4),
('SEM_RISCO_IDENTIFICADO', 0, 'Sem prioridade indicada pela escala', 'Escore inferior a 5: sem risco identificado pelos critérios da escala', 5);
