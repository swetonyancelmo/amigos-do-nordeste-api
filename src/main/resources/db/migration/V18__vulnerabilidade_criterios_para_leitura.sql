-- A justificativa de cada sentinela agora aparece na tela, na tabela "Como a
-- prioridade é calculada" (GET /api/vulnerabilidade/base, ADR-0010). Os textos
-- da V16 foram escritos para quem programa ("Critério da ADR-0010",
-- "familia.numero_comodos (V17)"); estes são para quem usa o painel.
-- Só texto: nenhum peso, faixa, corte ou parâmetro muda.
--
-- Atenção: os números citados aqui (18 a 59 anos, 70 anos, 6 meses) repetem
-- os parâmetros da V16. Mudou um parâmetro, atualize o texto junto.
--
-- Também é semente dos testes (application-test.yml): só UPDATE portável.

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Conta se a casa não tem banheiro, se o esgoto vai a céu aberto ou direto para rio, lago ou mar, se a água de beber não tem tratamento, ou se a água vem só de carro-pipa ou direto do rio. Cisterna e fossa rudimentar não contam: no sertão são a regra, não a exceção.'
 WHERE codigo = 'BAIXAS_CONDICOES_SANEAMENTO';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Conta se ninguém na família tem renda de trabalho (fixo, informal ou sazonal) e há pelo menos uma pessoa de 18 a 59 anos. Família que só recebe benefício conta; aposentado que mora sozinho, não.'
 WHERE codigo = 'DESEMPREGO';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Conta se há bebê com menos de 6 meses. Só a data de nascimento mostra isso: com idade estimada, que é em anos, não dá para saber.'
 WHERE codigo = 'MENOR_DE_SEIS_MESES';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Conta se alguém da família tem 70 anos completos ou mais.'
 WHERE codigo = 'MAIOR_DE_70_ANOS';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Número de pessoas da família dividido pelo número de cômodos da casa.'
 WHERE codigo = 'RELACAO_MORADOR_COMODO';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'O cadastro não pergunta se a pessoa sabe ler e escrever, e "estuda" não serve como substituto.'
 WHERE codigo = 'ANALFABETISMO';

UPDATE vulnerabilidade_sentinela SET justificativa =
 'Dado de saúde: pela LGPD, a associação não coleta nem guarda esse tipo de informação.'
 WHERE situacao = 'DESCARTADA_LGPD';
