# ADR-0010 — Classificador de vulnerabilidade familiar: Escala de Coelho-Savassi como sistema especialista

**Data:** 09/10/2026 · **Situação:** aceita para a API; rótulos, ordem dos
estratos e critérios de adaptação **a validar com a associação** antes de ir
para produção (ver "Decisões que ainda são humanas").

## Contexto

A associação atende mais de 2.500 famílias no Sertão do Moxotó com recursos
que não alcançam todas ao mesmo tempo. Hoje a decisão de quem recebe primeiro
é tomada de memória, por quem conhece as famílias. Funciona enquanto essa
pessoa está presente e lembra de tudo, mas não se explica a um doador, não se
audita e não sobrevive a uma troca de equipe.

O cadastro já guarda o necessário para parte de uma avaliação: composição
familiar com idades, condições de moradia e saneamento (com as categorias da
Ficha de Cadastro Domiciliar do e-SUS, ADR-0003) e fontes e faixa de renda.

O resultado desta funcionalidade vai orientar quem recebe doação. Por isso
duas exigências guiam a decisão inteira: **o critério precisa ser
defensável** (não pode ser um peso inventado pelo grupo) e **o resultado
precisa se explicar** (ninguém decide uma doação olhando um "7").

## Decisão

Implementar um **sistema especialista baseado em regras** que aplica a
**Escala de Risco Familiar de Coelho-Savassi**, adaptada ao que este cadastro
pode e deve coletar, e que devolve para cada família uma **sugestão** de
estrato com a explicação de como se chegou a ela.

### Por que regras, e não aprendizado de máquina

1. **Não existe dado rotulado.** Um modelo supervisionado precisaria de
   famílias já classificadas por alguém ("esta era prioridade, esta não"). A
   associação não tem esse histórico, e criá-lo agora seria pedir à equipe que
   rotule de memória — exatamente o critério que se quer tornar explícito.
   Treinar sobre esse rótulo reproduziria o viés de quem rotulou, com
   aparência de objetividade.
2. **Um modelo que não explica a priorização é indefensável aqui.** Quando
   uma família pergunta por que não recebeu, a resposta precisa ser "porque o
   cadastro indica X e Y, que valem tantos pontos neste instrumento", nunca
   "porque o modelo disse". A LGPD garante ao titular o direito de pedir
   revisão de decisão tomada unicamente por tratamento automatizado (art. 20);
   este sistema não toma decisão nenhuma, e cada sugestão vem com a regra que
   a produziu.
3. **Já existe instrumento validado.** Não é preciso aprender um critério que
   a Atenção Primária brasileira já publicou, aplicou e discutiu por duas
   décadas.

O sistema não traz biblioteca de aprendizado de máquina nem chama modelo de
linguagem. É um sistema de regras de propósito.

### As três partes do sistema especialista

A separação entre conhecimento e inferência é o que define um sistema
especialista, e é ela que permite à associação ajustar a avaliação sem
programador.

| Parte | Onde está | O que faz |
|---|---|---|
| **Base de conhecimento** | tabelas `vulnerabilidade_sentinela`, `vulnerabilidade_faixa`, `vulnerabilidade_parametro` e `vulnerabilidade_estrato` (migração V15, conteúdo na V16) | sentinelas, pesos, faixas, cortes dos estratos, rótulos de exibição e os parâmetros de adaptação (quais valores contam como precário, limites de idade) |
| **Motor de inferência** | `vulnerabilidade/motor/MotorDeInferencia.java` | lê as regras, avalia a família, soma, estratifica. Puro: sem banco, sem relógio, sem estado, e testado isoladamente |
| **Facilidade de explicação** | `Avaliacao` (resposta da API) | para cada família: sentinelas presentes com os pontos de cada uma, as ausentes, as indeterminadas e os campos que faltam preencher |

O único conhecimento que fica em código é **como ler a família**: traduzir
"`escoamentoSanitario = CEU_ABERTO`" no fato "escoamento precário"
(`Avaliadores.java`). Mesmo aí, *quais* valores contam vem da base. Na
prática, a coordenação pode dizer "aqui no sertão, depender de carro-pipa pesa
mais" e isso vira um `UPDATE` na tabela, não um deploy. A base é lida a cada
requisição, sem cache, e é validada ao carregar: um parâmetro que não é valor
de enum ou cortes fora de ordem fazem a avaliação falhar com a causa na
mensagem, em vez de classificar com uma base quebrada.

## O instrumento

> COELHO, Flávio Lúcio G.; SAVASSI, Leonardo Cançado Monteiro. Aplicação de
> Escala de Risco Familiar como instrumento de priorização das Visitas
> Domiciliares. **Revista Brasileira de Medicina de Família e Comunidade**,
> Rio de Janeiro, v. 1, n. 2, p. 19-26, 2004.
> DOI: [10.5712/rbmfc1(2)104](https://doi.org/10.5712/rbmfc1(2)104).

A escala foi construída sobre a **Ficha A do SIAB**, o cadastro domiciliar
que as equipes de Saúde da Família já preenchiam. Ela é antecessora da Ficha
de Cadastro Domiciliar do e-SUS, de onde vêm as categorias de água e esgoto
deste projeto (ADR-0003). O parentesco entre as fontes de dado é a razão
prática da escolha: o instrumento foi desenhado para ser aplicado a um
cadastro como o nosso, sem formulário novo.

### Sentinelas e pontos (Tabela 01 do artigo)

| Dados da Ficha A | Escore |
|---|---|
| Acamado | 3 |
| Deficiência Física | 3 |
| Deficiência mental | 3 |
| Baixas condições de saneamento | 3 |
| Desnutrição (Grave) | 3 |
| Drogadição | 2 |
| Desemprego | 2 |
| Analfabetismo | 1 |
| Menor de seis meses | 1 |
| Maior de 70 anos | 1 |
| Hipertensão Arterial Sistêmica | 1 |
| Diabetes Mellitus | 1 |
| Relação morador/cômodo: se maior que 1 | 3 |
| Relação morador/cômodo: se igual a 1 | 2 |
| Relação morador/cômodo: se menor que 1 | 0 |

A relação morador/cômodo não é sentinela de "tem/não tem": é uma razão que
cai numa de três faixas. Na base ela é uma regra do tipo `FAIXA`, com as três
faixas em `vulnerabilidade_faixa`. As demais são do tipo `BINARIA`.

### Estratificação (Quadro 02 do artigo)

| Escore total | Risco familiar |
|---|---|
| 5 ou 6 | R1 — risco menor |
| 7 ou 8 | R2 — risco médio |
| 9 ou mais | R3 — risco máximo |

Dois pontos de fidelidade ao texto original:

- O Quadro 02 escreve literalmente "Maior que 9 = (R3)", o que deixaria o
  escore 9 sem estrato. A Tabela 2 do mesmo artigo usa "Escore 9 ou maior
  (R3)", e o texto da Cena 2 fala em "escore igual ou superior a 9, sendo
  classificadas como R3". Adotamos **9 ou mais**, que é a leitura coerente
  com os resultados que os autores publicaram.
- O artigo não dá código para escore abaixo de 5. Ele aparece na Tabela 2 como
  "Escore Inferior a 5", e essas famílias não são classificadas como risco.
  Aqui ele é o estrato `SEM_RISCO_IDENTIFICADO`, com código próprio e sem
  letra R, para ninguém confundir com um estrato do instrumento.

### O artigo prevê a adaptação

Na seção 2.4, os autores descrevem uma etapa de equipe antes de aplicar a
escala:

> "Define-se então quais eventos serão pontuados o que deve ser
> individualizado de acordo com a comunidade a se abordar. Por exemplo, em uma
> comunidade onde todas as pessoas não têm saneamento básico, este dado será
> incluído na situação de área de risco, ao invés de situação de risco
> familiar." (COELHO; SAVASSI, 2004, p. 24)

Adaptar o instrumento ao contexto é, portanto, parte do método, não um desvio
dele, desde que o corte esteja justificado por escrito. É isso que esta seção
faz. **Nenhum peso e nenhum corte foram alterados**; o que se adaptou foi
*quais sentinelas entram* e *como cada uma é lida* no cadastro.

## A adaptação ao cadastro

Cada sentinela tem uma linha na base, inclusive as que não entram no escore,
com a situação (`AVALIADA`, `NAO_COLETADA`, `DESCARTADA_LGPD`) e a
justificativa. O corte fica registrado no próprio dado e aparece no
relatório.

### Avaliadas com os dados que já existiam

**Baixas condições de saneamento (3).** O artigo não define o termo nas
categorias do e-SUS. A Ficha A do SIAB, que é a origem da sentinela, trazia
três blocos: tratamento da água no domicílio (com a opção "sem tratamento"),
abastecimento (rede geral, poço ou nascente, outros) e destino de fezes e
urina (rede, fossa, céu aberto). O critério adotado traduz as piores opções
desses blocos para o e-SUS:

> **Presente** quando ao menos um destes é verdadeiro: a família **não tem
> banheiro**; o escoamento é **a céu aberto** ou **direto para rio, lago ou
> mar**; a água de beber é **sem tratamento**; ou a água vem **só** de
> carro-pipa ou de captação direta de rio.

- Basta um componente precário. Para concluir **ausente**, todos os
  componentes precisam estar informados.
- **Cisterna não conta.** No semiárido a cisterna é a regra, não a exceção.
  Contá-la marcaria quase todas as famílias e, como diz a seção 2.4, isso é
  risco da área, não da família. Pelo mesmo motivo, fossa rudimentar também
  não conta.
- **"Só carro-pipa" conta**: a família sem nenhuma reserva própria depende
  inteiramente de entrega externa. É o mesmo indicador que o relatório de
  situação já usa (`soCarroPipa`, ADR-0003).
- Os valores de cada componente são parâmetros da base
  (`ESCOAMENTO_PRECARIO`, `TRATAMENTO_PRECARIO`, `ABASTECIMENTO_SO_DE`). Um
  componente sem valores fica desligado e deixa de ser exigido.

**Desemprego (2).** O cadastro não guarda ocupação por pessoa, que era o campo
da Ficha A. O que existe são as fontes de renda da família. O critério é um
**substituto declarado**:

> **Presente** quando a família não tem **nenhuma fonte de renda de trabalho**
> (fixo, informal ou sazonal) **e** tem ao menos uma pessoa de **18 a 59
> anos**.

- Família que **só recebe benefício** (Bolsa Família, BPC, aposentadoria,
  pensão) e tem adulto em idade ativa pontua. Família de aposentados sem
  ninguém em idade ativa não pontua: aposentado não é desempregado.
- Trabalho sazonal conta como trabalho. É uma escolha discutível no sertão,
  onde a entressafra é desemprego de fato; por isso as fontes que contam como
  trabalho são parâmetro da base (`FONTE_DE_TRABALHO`).
- **A faixa de renda não decide desemprego.** Ela só distingue "nenhuma fonte
  cadastrada porque não há renda" (`SEM_RENDA_FIXA`) de "ninguém preencheu"
  (faixa nula, que deixa a sentinela indeterminada).
- O limite de 18 anos (maioridade) e o de 59 (60 é idoso pelo Estatuto da
  Pessoa Idosa) são parâmetros `IDADE_ATIVA_MINIMA` e `IDADE_ATIVA_MAXIMA`.

**Maior de 70 anos (1).** Pessoa com **70 anos completos ou mais**, pelo
cálculo de idade que o projeto já usa (`dominio/Idade`, com data de nascimento
ou idade estimada datada). Quem completou 70 já viveu mais de 70 anos; exigir
71 deixaria de fora quase um ano de pessoas que o artigo descreve.

**Menor de seis meses (1).** Só a **data de nascimento** enxerga meses: a
idade estimada é em anos. Quando alguém da família não tem data de nascimento,
a sentinela fica **indeterminada**, não ausente, mesmo que a estimativa seja de
um adulto. É a regra combinada; ver "Decisões que ainda são humanas" para um
refinamento possível.

### Acrescentada: número de cômodos

**Relação morador/cômodo (0, 2 ou 3).** Migração V17: `familia.numero_comodos`,
inteiro, nulo permitido (nenhum cadastro antigo tem esse dado) e sempre maior
que zero quando informado. Moradores são as pessoas da família (ADR-0003:
família = domicílio). Esse número é contado na hora, nunca vira coluna. A
comparação é exata (`moradores ? 1 × cômodos`), sem divisão, para "igual a 1"
não depender de arredondamento.

É o maior ganho por esforço da lista inteira: uma pergunta de campo trivial
("quantos cômodos tem a casa?"), que vale até 3 pontos e que o próprio artigo
destaca como "importante indicador na avaliação do risco".

### Fora do escore

**Analfabetismo (1): não coletada.** A Ficha A perguntava "alfabetizado"
para quem tinha 15 anos ou mais. Este cadastro não pergunta. O campo `estuda`
**não** é alfabetização (um adulto que não estuda pode ler, e uma criança que
estuda pode ainda não ler) e não é usado como substituto. Fica fora até
existir um campo "sabe ler e escrever" por pessoa adulta.

**Acamado, deficiência física, deficiência mental, desnutrição grave,
drogadição, hipertensão e diabetes: descartadas por LGPD.** São **dados
pessoais sensíveis** referentes à saúde (Lei 13.709/2018, art. 5º, II), cujo
tratamento só é permitido nas hipóteses estritas do art. 11. Uma associação
civil guardar diagnóstico de pessoa em situação de vulnerabilidade, num
sistema que a própria associação disse temer ver usado em golpe
(`requisitos.md`), é uma responsabilidade de outra categoria. Não é "mais um
campo". O sistema **não cria campo, coluna, enum ou rota** para nenhum desses
dados, e a avaliação não tenta inferi-los de outros campos.

Na base essas sentinelas existem só como linha do instrumento, com o peso
original e a justificativa. Nenhum dado de pessoa está ligado a elas.

### A consequência aritmética, dita com honestidade

| | Escore máximo |
|---|---|
| Instrumento original (todas as 13 sentinelas) | 27 |
| Descartadas por LGPD | −16 |
| Analfabetismo (não coletada) | −1 |
| **Alcançável com o número de cômodos** | **10** |
| Alcançável sem o número de cômodos | 7 |

Sem o campo de cômodos, R3 (9) é **inalcançável** e R2 (7) só é atingido com
todas as sentinelas restantes presentes. Com ele, o teto volta a 10 e os três
estratos voltam a ser alcançáveis como o instrumento foi desenhado.

**Os cortes 5, 7 e 9 foram mantidos.** Reescalá-los para "compensar" as
sentinelas ausentes seria inventar um instrumento novo, sem validação, e jogar
fora a única razão de usar Coelho-Savassi. A consequência precisa ser
assumida: **esta avaliação é um limite inferior do risco.** Uma família em
`SEM_RISCO_IDENTIFICADO` pode ter um acamado, uma criança desnutrida ou um
idoso diabético que a escala original pontuaria. Por isso a resposta da API
traz `escoreMaximoAlcancavel`, e o relatório lista as sentinelas não
avaliadas com o motivo: quem lê o resultado precisa saber o que ele não vê.

## As cinco regras

### 1. Dado faltando não é dado bom

É o erro mais grave possível aqui, e ele aconteceria: as famílias importadas
das planilhas antigas chegam sem saneamento, sem renda e sem idades. Se a
ausência somasse zero, elas cairiam abaixo de 5 e sairiam como "sem risco",
exatamente ao contrário da realidade.

Cada sentinela termina num de três estados:

| Estado | Significa | No escore |
|---|---|---|
| **Presente** | o dado existe e dispara a sentinela | soma os pontos |
| **Ausente** | o dado existe e diz que não | soma zero |
| **Indeterminada** | o dado não existe | não soma, e conta para o critério abaixo |

**Critério de dados insuficientes.** O motor calcula o estrato duas vezes:
com as indeterminadas valendo zero e com elas valendo o máximo que podem
somar. **Se os dois estratos coincidem, completar o cadastro não mudaria a
classificação**, e a família recebe estrato e escore. **Se diferem, a família
não recebe escore nem estrato**: recebe `DADOS_INSUFICIENTES`, com a lista de
campos que faltam (`camposFaltantes`).

Esse critério de "mínimo" não depende de uma lista arbitrária de campos
obrigatórios. Ele é, por construção, o menor conjunto de dados que torna a
resposta verdadeira. Três consequências:

- uma família sem nenhum dado vai sempre para `DADOS_INSUFICIENTES` (entre 0
  e 10 pontos possíveis, cruza todos os cortes);
- uma família que já soma 9 é R3 mesmo com dado faltando, porque completar só
  pode subir;
- uma família com 0 pontos e só o tratamento de água em branco é
  `SEM_RISCO_IDENTIFICADO`, porque nem os 3 pontos possíveis a levariam a 5.

`DADOS_INSUFICIENTES` **pede ação** (completar o cadastro) e **nunca é tratado
como prioridade baixa**: aparece no relatório com contagem e com os campos que
mais faltam, é filtrável na lista (`?estrato=DADOS_INSUFICIENTES`) e, na
ordenação por prioridade, fica acima de `SEM_RISCO_IDENTIFICADO`. Os pontos já
confirmados (`pontosConfirmados`) vêm na resposta, para quem lê saber que uma
família "a completar" pode já somar 7.

### 2. É sugestão, nunca decisão

O sistema classifica e explica; quem decide é a pessoa. Nada parte do escore
automaticamente: ele não reordena fila, não seleciona beneficiário, não
dispara aviso. A ordenação `?ordenacao=PRIORIDADE` só existe quando alguém a
pede, e o padrão da lista continua sendo por nome.

### 3. O escore é calculado, nunca armazenado

Mesma regra de todos os totais do projeto. Mudou a regra, muda o escore de
todo mundo na próxima leitura, como tem que ser. A consequência técnica é que
filtrar ou ordenar a lista por estrato exige avaliar todas as famílias que
passam pelos outros filtros e cortar a página em memória (para ~2.500 famílias,
algumas dezenas de consultas pequenas, sem N+1).

### 4. O rótulo é texto, e texto estigmatiza

"R3 — risco máximo" faz sentido num prontuário, não numa folha levada para
dentro da comunidade. O **código** do estrato continua fiel ao instrumento
(`R1`, `R2`, `R3`), porque é ele que permite citar o artigo. O **rótulo de
exibição** é outra coluna da base, configurável, servida em `/api/metadados`
e em cada avaliação. A proposta inicial prioriza sem rotular pessoa:

| Código | Rótulo proposto | Descrição no instrumento |
|---|---|---|
| `R3` | Maior necessidade de apoio | R3 — risco máximo (escore 9 ou mais) |
| `R2` | Necessidade intermediária de apoio | R2 — risco médio (escore 7 ou 8) |
| `R1` | Necessidade de apoio a acompanhar | R1 — risco menor (escore 5 ou 6) |
| `DADOS_INSUFICIENTES` | Completar cadastro para avaliar | fora do instrumento |
| `SEM_RISCO_IDENTIFICADO` | Sem prioridade indicada pela escala | escore inferior a 5 |

O último rótulo diz "pela escala" de propósito: a escala adaptada não vê saúde
(ver acima), e o texto não pode sugerir que a família não precisa de nada.

### 5. Nada de dado de saúde

Nenhum campo, coluna, enum ou rota para diagnóstico, deficiência, desnutrição
ou uso de substância. Se o cálculo pedir um desses dados, a sentinela fica
fora e o corte é relatado, como foi feito com as sete do Grupo C.

## Onde aparece

| Rota | O que traz |
|---|---|
| `GET /api/familias/{id}` | `numeroComodos` e `vulnerabilidade`: estrato, rótulo, escore, pontos confirmados e em aberto, teto, explicação completa e campos faltantes |
| `GET /api/familias` | `vulnerabilidade` resumida em cada linha (estrato, rótulo, escore, pontos confirmados); filtros `estrato` (repetível) e `ordenacao=NOME\|PRIORIDADE` |
| `GET /api/relatorios/vulnerabilidade` | distribuição por estrato no total, por município e por comunidade, com percentual; campos que mais faltam nas famílias `DADOS_INSUFICIENTES`; sentinelas não avaliadas e o motivo; só contagens, nenhum nome |
| `GET /api/vulnerabilidade/base` | as regras em uso (sentinelas e pontos, faixas, cortes de cada estrato, sentinelas fora da conta), lidas e validadas como o motor as usa. É o que a tela mostra como "de onde vem esta prioridade": se um peso for ajustado, a tabela muda junto, sem fingir que é o artigo puro |
| `GET /api/metadados` | `estratoVulnerabilidade`: código e rótulo da base, na ordem de prioridade |
| `POST`/`PUT /api/familias` | aceitam `numeroComodos` (1 a 99, ou nulo) |

Todas, menos metadados, são `ROLE_ADMIN`. O token do aparelho da agente não
alcança nenhuma (testado). **O escore não entra na exportação em Excel**, que
leva nome de família: juntar os dois num arquivo que circula é decisão do
grupo, não do código.

## Histórico: proposta, não implementada

Se o grupo quiser responder "essa família melhorou?", será preciso guardar
avaliações, e um escore guardado só faz sentido com **a versão da base que o
produziu**: o mesmo 7 com pesos diferentes não é o mesmo 7. A proposta é:

- `vulnerabilidade_versao` (id, criada_em, descricao), com a base atual como
  versão 1, e uma versão nova a cada mudança de peso, corte ou parâmetro;
- `avaliacao_registrada` (familia_id, versao_id, estrato, escore,
  avaliada_em), gravada **só por ação explícita** (ex.: "registrar avaliação
  desta campanha"), nunca a cada leitura;
- comparações só entre avaliações da mesma versão, ou reavaliando as antigas
  com a versão atual.

Isso abre exceção à regra 3 e cria um dado novo sobre cada família. Fica para
decisão do grupo.

## Decisões que ainda são humanas

Antes de produção:

1. **Rótulos de exibição.** A tabela acima é proposta. Precisa ser lida pela
   coordenação, de preferência pensando em como soaria lida em voz alta na
   comunidade.
2. **Posição de `DADOS_INSUFICIENTES` na ordenação.** Está entre R1 e
   `SEM_RISCO_IDENTIFICADO`: nunca abaixo de quem foi avaliado sem risco, mas
   sem enterrar R3 sob centenas de cadastros antigos. Alternativa razoável:
   logo depois de R3. É a coluna `ordem` da base.
3. **Critério de saneamento**: cisterna e fossa rudimentar ficaram de fora e
   "só carro-pipa" entrou, por leitura da seção 2.4 do artigo para o
   semiárido. Precisa de confirmação de quem conhece as comunidades.
4. **Trabalho sazonal conta como trabalho?** Hoje sim.
5. **Menor de seis meses com idade estimada.** Hoje qualquer pessoa sem data de
   nascimento deixa a sentinela indeterminada, mesmo um adulto estimado em 40
   anos. Um refinamento defensável seria descartar quem tem estimativa de 2
   anos ou mais. Ele reduziria muito as famílias em `DADOS_INSUFICIENTES` por
   esse motivo, mas mexe numa regra combinada e por isso não foi feito.
6. **Onde preencher cômodos.** A API aceita o campo, mas o formulário do web
   ainda não o mostra, e o PUT atual do web, que não envia o campo, **apaga o
   valor** (o PUT substitui todos os campos simples). O web precisa incluir
   `numeroComodos` no formulário antes de alguém preenchê-lo por outro
   caminho. O app e o corpo de `/aprovar` também não perguntam.
7. **Edição da base pelo painel.** Hoje mudar peso ou rótulo é um `UPDATE` no
   banco (ou uma migração nova). Não há tela nem rota para isso, de propósito:
   mudar o instrumento deveria passar pela ADR.
8. **Escore em exportação com nome** (ver "Onde aparece").

## Consequências

- A associação ganha um critério explícito, citável e auditável para uma
  decisão que hoje é tomada de memória, sem que o sistema tome a decisão.
- O resultado é honesto sobre o que não sabe: falta de dado vira tarefa
  ("completar cadastro"), não "sem risco".
- O teto de 10 pontos e a ausência das sentinelas de saúde tornam a avaliação
  um limite inferior do risco. Isso precisa ser dito sempre que o resultado for
  apresentado.
- O número de cômodos passa a ser o dado mais valioso a coletar, e é o melhor
  argumento para incluí-lo no formulário do web e no app.
- Mudar a avaliação passa a ser mudar dado. Isso é poder: deve vir
  acompanhado de registro aqui.

## Referências

- COELHO, F. L. G.; SAVASSI, L. C. M. Aplicação de Escala de Risco Familiar
  como instrumento de priorização das Visitas Domiciliares. *Rev. Bras. Med.
  Fam. Comunidade*, v. 1, n. 2, p. 19-26, 2004. DOI 10.5712/rbmfc1(2)104.
- BRASIL. Lei nº 13.709, de 14 de agosto de 2018 (Lei Geral de Proteção de
  Dados Pessoais), arts. 5º, II; 11; 20.
- ADR-0003 (renda e moradia, categorias do e-SUS) e ADR-0005 (mapa por
  comunidade, privacidade) deste repositório.
