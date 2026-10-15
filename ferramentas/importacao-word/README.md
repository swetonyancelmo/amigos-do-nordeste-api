# Importação dos cadastros em Word

Leva para a API as famílias que a associação registra hoje em tabelas de Word
(uma por sítio ou povoado, feitas para a campanha de Natal). A ideia é que o
sistema não seja entregue vazio.

```
.docx ─► [1] extrair.py ─► extraido.xlsx ─► [2] validar.py ─► revisao.xlsx ─► REVISÃO HUMANA ─► [3] enviar.py ─► API
                                                    └─► relatorio-validacao.md
```

O meio do caminho é planilha, não JSON, porque quem revisa é a responsável
pela associação. Tudo passa pela API (nunca direto no banco), porque é lá que
moram as regras.

> **Dado pessoal.** Os `.docx`, as planilhas, o `controle-envio.json` e os
> relatórios de `trabalho/` têm nome, CPF e composição familiar de pessoas
> reais. A pasta `trabalho/` e qualquer `.docx`/`.xlsx`/`.json` desta pasta
> estão no `.gitignore`. **Depois da importação, apague a pasta `trabalho/`**
> (e as cópias dos `.docx` fora do lugar de origem). Os scripts nunca imprimem
> nome nem CPF: uma linha é sempre citada pela origem (arquivo · sítio ·
> tabela · linha).

## Preparar

```bash
cd ferramentas/importacao-word
python3 -m venv .venv && . .venv/bin/activate
pip install -r requirements.txt
```

## 1. Extrair (um arquivo por vez)

O documento não diz o município nem a data em que foi feito: os dois entram
pela linha de comando e são obrigatórios.

```bash
python extrair.py "/caminho/CADASTRO NATAL SÍTIO GILA.docx" \
    --municipio "Nome do Município" --uf PE --data-referencia 2024-11-28
```

- `--data-referencia` é a data **do documento**, não a de hoje. Se uma idade
  anotada em 2024 entrasse com a data de hoje, o cadastro nasceria anos errado.
  Não sabe? Passe `desconhecida`: a extração roda, mas o envio fica bloqueado
  até alguém perguntar à associação.
- `--municipio desconhecido` funciona do mesmo jeito.
- Rodar de novo para o mesmo arquivo troca as linhas dele (com `--substituir`
  se elas já tiverem revisão). Um arquivo com o mesmo conteúdo de outro já
  extraído (ex.: `... (1).docx`) é recusado.
- Se o documento não estiver no formato conhecido (formato A, abaixo), o
  script **para** e não grava nada. Não adapte o parser sem olhar o arquivo.

## 2. Validar

```bash
python validar.py                                   # extraido.xlsx -> revisao.xlsx
```

Gera `trabalho/revisao.xlsx` e `trabalho/relatorio-validacao.md`. O relatório
só tem números e exemplos sem identificação: é o que vai para a apresentação.

### Como revisar a planilha

Na aba `formato_A`, as células com problema estão pintadas e comentadas:
**vermelho bloqueia** o envio daquela família, **amarelo** só avisa. A coluna
`problemas` resume tudo. Só as colunas de **cabeçalho azul** são para editar:

| Coluna | Para quê |
|---|---|
| `comunidade_nome`, `comunidade_tipo` | Proposta tirada do cabeçalho ("SÍTIO GILA" → "Gila", `SITIO`). Para juntar com uma comunidade que já existe, escreva o nome exatamente igual. |
| `comunidade_nova_confirmada` | `SIM` quando a comunidade é nova mesmo, apesar de ter nome parecido com outra ("Sítio Igrejinha" × "Igrejinha"). |
| `meninos_ate_12`, `meninas_ate_12` | Quantidade de crianças por sexo. Vem preenchida só quando "01 M"/"03 F" cabe no total. Só é usada com `--criancas-como-contagem` (ver decisões). |
| `acao` | Vazio = importar; `PULAR` = não importar esta linha; `CONFIRMADO` = nome igual ao de outra linha, mas são famílias diferentes. |

Para corrigir um CPF, use o papel. Se não der, apague a célula: a família
entra sem CPF. As colunas de texto cru também podem ser corrigidas pelo papel.

Depois de revisar, valide de novo **sobre a própria revisão** (o que foi
editado nas colunas azuis é mantido):

```bash
python validar.py --entrada trabalho/revisao.xlsx
```

## 3. Enviar

```bash
export IMPORTACAO_API_URL=https://cadastro-familias-api.onrender.com   # padrão: http://localhost:3333
export IMPORTACAO_EMAIL=...       # a conta do painel
read -rs IMPORTACAO_SENHA && export IMPORTACAO_SENHA

python enviar.py                  # SIMULA: monta tudo, não envia nada (padrão)
python enviar.py --enviar         # envia de verdade
```

As credenciais só são lidas do ambiente, nunca de argumento (que fica no
histórico do shell). Na simulação, com credenciais, a API é consultada só para
leitura; sem elas, a simulação roda offline.

O que é enviado para cada família (`POST /api/familias`):

- responsável com nome, CPF (só se o dígito verificador bate) e
  `parentesco: RESPONSAVEL`;
- mais `N − 1` pessoas **sem nome, sem idade e sem sexo**, com
  `cadastroIncompleto: true`, onde `N` é o "Total de Pessoas" do documento.
  Assim o sistema mostra o total certo sem inventar nada, e o filtro
  "incompleto" da tela de Pessoas vira a lista de trabalho da associação;
- em `observacoes`, o **texto original** da linha (total, faixas, crianças,
  gestante) e a origem. Nada se perde.
- Nenhum total é enviado: o sistema conta as pessoas. Nenhuma idade é
  inventada para caber em faixa etária.
- Total "XX" ou em branco: entra só o responsável.

Pode morrer no meio. `POST /api/familias` não é idempotente, então
`trabalho/controle-envio.json` guarda origem → id de cada família criada, e a
próxima execução pula o que já passou. Antes de cada POST a origem é marcada
`em_andamento`; se a rede cair nesse instante, não dá para saber se a família
foi criada, e ela **não** é reenviada sozinha. Confira na tela de Famílias e,
se não estiver lá, rode com `--reenviar-pendentes`. O controle é de uma API
só: para outro ambiente, use outro `--controle`.

Município e comunidade são resolvidos por nome (sem acento e sem caixa) e
criados pela API quando não existem. Nome parecido mas diferente bloqueia a
comunidade até a revisão decidir; nunca é mesclado sozinho.

O relatório final (`trabalho/relatorio-envio.md`) traz famílias e pessoas
criadas, puladas, bloqueadas e as que falharam, com o motivo e a origem. A
mensagem de erro da API não é repassada (a do 409 de CPF repetido traz o nome
do responsável); sai só o status e, no 400, os nomes dos campos.

## Formatos conhecidos

### Formato A: folha de contagem por família

Uma tabela de cabeçalho de 4×2 (`SÍTIO OU ASSENTAMENTO`, `CONTATO`,
`TOTAL DE FAMÍLIAS`, `TOTA DE CRIANÇAS`, com o erro de digitação do original)
seguida da tabela de famílias: `Qtd Família`, `Responsável`,
`Total de Pessoas`, `Maior de 60`, `>13<60`, `Criança até 12 anos` (mesclada
em duas sub-colunas sem título), `Gestante`, `CPF`. Não há pessoa individual:
só contagens.

A leitura é feita pelo XML da grade (`w:tc` + `gridSpan`), não por
`row.cells`, que repete o texto de célula mesclada. O script aceita mais de
um sítio por arquivo (cabeçalho, tabela, cabeçalho, tabela…).

### Formato B: cadastro por pessoa

Mencionado (Sítio Igrejinha, com roupa e calçado), mas **nenhum arquivo nesse
formato foi recebido**. O parser não existe: o `extrair.py` reconhece colunas
de roupa/calçado/idade e para. Escreva o parser só depois de abrir o arquivo.

## Estrutura dos documentos recebidos (Fase 0, 13/10/2026)

Oito arquivos `CADASTRO NATAL …docx`. `POVOADO CAMPOS (1).docx` é cópia byte a
byte de `POVOADO CAMPOS.docx` e foi descartada. Os sete restantes são todos do
**formato A**, com 2 tabelas e 1 sítio cada:

| Documento | Sítio no cabeçalho | Famílias | Linhas numeradas vazias | Criado / modificado (metadado) |
|---|---|---:|---:|---|
| SÍTIO SERROTA 01 | SERROTA 01 | 17 | 35 | 25/11/2024 |
| SÍTIO JATOBÁ 01 | JATOBÁ 01 | 49 | 3 | 08/11/2024 – 25/11/2024 |
| SÍTIO HENRIQUE | SÍTIO HENRIQUE | 23 | 29 | 26/11/2024 (UTC 27/11) |
| SÍTIO GERÔNIMO | SÍTIO GERÔNIMO | 16 | 36 | 26/11/2024 (UTC 27/11) |
| SÍTIO JORGE | SÍTIO JORGE | 37 | 15 | 28/11/2024 |
| SÍTIO GILA | SÍTIO GILA | 53 | 0 | 28/11/2024 |
| POVOADO CAMPOS | POVOADO CAMPOS | 38 | 15 | 28/11/2024 (UTC 29/11) |
| **Total** | | **233** | 133 | |

Em todos: cabeçalho de totais e contato em branco, colunas "Maior de 60" e
">13<60" vazias em todas as linhas. Em JATOBÁ 01 a coluna de crianças está
vazia em todas as linhas. O nome "01" em SERROTA e JATOBÁ sugere outras folhas
(02, 03…) que não estavam entre os arquivos recebidos.

## Decisões em aberto

1. **O que "XX" significa** (total de pessoas, crianças, CPF como "XXX").
   Hoje é tratado como desconhecido.
2. **O que a coluna "Criança até 12 anos" registra.** Era para ser
   quantidade e sexo ("01 M"), mas em SÍTIO GILA e SÍTIO JORGE há valores que
   só podem ser idade ("08 F" numa família de 2, "10 M" numa família de 4,
   "4 – 7", "1 A E 6M"). "01 M" pode ser um menino ou um menino de 1 ano.
   Enquanto não houver resposta, o envio **não atribui sexo** a ninguém; com a
   confirmação, rode com `--criancas-como-contagem`.
3. **Data de referência e município de cada documento.** Os metadados
   apontam novembro de 2024, mas isso não é confirmação.
4. **Os "01" de SERROTA e JATOBÁ** fazem parte do nome da comunidade?

Comportamento da API a saber (não foi contornado): o servidor marca como
`cadastroIncompleto` toda pessoa sem informação de idade, inclusive o
responsável; e recusa CPF com dígito verificador errado.
