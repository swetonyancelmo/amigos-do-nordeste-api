# Requisitos

Fontes: reunião com a dona da associação (26/08/2026), mensagem posterior sobre
renda e moradia (25/08), e o formulário de elicitação respondido por **Karla
Espinhara, coordenadora de projeto, há mais de 3 anos na associação** (31/08).

---

## Questões em aberto — resolver antes de codar as telas

Não são detalhes. Cada uma muda o que precisa ser construído.

### Q-01 · Quantas pessoas realmente vão usar o sistema? **(bloqueante)**

Há uma contradição entre as duas fontes:

| Fonte | O que diz |
|---|---|
| Dona da associação, na reunião | "Apenas 1 pessoa realiza o cadastro e apenas 1 pessoa tem acesso" |
| Karla, no formulário | "Mais de 100" pessoas colaboram com a associação |

As duas podem ser verdade — colaborar com a associação não é o mesmo que operar
o sistema. Mas **a resposta muda a autenticação inteira**: um usuário só (como
está construído hoje) ou perfis com permissões diferentes.

Outras respostas da Karla que puxam para o segundo cenário: o cadastro de
voluntários é feito por formulário, informações são compartilhadas em reuniões,
e ela mesma marcou que **existem informações que deveriam ter acesso restrito**
("informações administrativas e que infrinjam a LGPD") — o que só faz sentido se
mais de uma pessoa acessa.

**Como perguntar:** *"Quantas pessoas vão digitar cadastro no sistema? E quantas
vão só consultar?"* — separar digitar de consultar é o que resolve.

**Enquanto não houver resposta:** seguimos com acesso único, que foi o que a
dona pediu. A `ADR-0002` já registra que a decisão está sob revisão.

### Q-02 · A prioridade é o cadastro ou as fotos? **(bloqueante)**

Karla respondeu **duas vezes**, em perguntas diferentes, que o maior problema é
outro:

> "Um programa capaz de armazenar as fotos de forma independente."
> "Um programa capaz de unir os registros de fotos e vídeos. Atualmente está tudo disperso."

E confirmou que já perderam arquivo importante — **"fotos dos nossos registros"**.

Isso não é o sistema que estamos construindo. Ver [ADR-0008](decisoes/ADR-0008-fotos-e-videos.md)
para a análise e a proposta.

### Q-03 · Onde o cadastro é digitado?

Karla diz que a internet da sede é **boa**, mas que *"a internet na área rural é
complexa e no deslocamento não temos acesso"*. Se a digitação acontece só na
sede, o sistema online resolve. Se alguém abrir no sítio, não.

Também marcou que a quantidade de equipamentos **não é suficiente**.

### Q-04 · Campanhas entram no escopo?

Perguntada sobre a atividade mais difícil, respondeu: *"Todas têm pontos de
dificuldade, mas **as campanhas dão muito trabalho**"*. O cadastro existe para
servir a campanha. Ver [ADR-0009](decisoes/ADR-0009-campanhas.md).

---

## O que o formulário confirmou

Isto é evidência a favor do que já está construído — vale citar na apresentação.

| Achado | Resposta literal |
|---|---|
| O cadastro é o retrabalho | "Existe tarefa que gera retrabalho? **Sim** → Cadastro famílias e crianças" |
| E é o que ela quer acelerar | "Qual atividade gostaria que fosse mais rápida? → **Cadastro famílias voluntários**" |
| A prioridade é organizar informação | "Se pudesse resolver só um problema: **Organização das informações**" |
| Existe duplicação | "A mesma informação precisou ser registrada em mais de um lugar? **Às vezes**" |
| Ferramentas atuais | Planilhas, documentos de texto, WhatsApp, Google Drive, documentos físicos |
| Backup é frágil | "Sim, mas **sem frequência definida**" — e já perderam arquivos |
| Ninguém mantém tecnologia internamente | "Quem resolve problema de tecnologia? **Uma pessoa de fora da associação**" |
| LGPD já é preocupação deles | Acesso restrito para "informações administrativas e que infrinjam a LGPD" |
| Segurança preocupa | "Principal preocupação: **golpes com nossos dados e contas**" |

Dois achados reforçam decisões que já estavam tomadas:

- **A exportação para Excel deixa de ser desejável e vira obrigatória.** Backup
  sem frequência definida + histórico de perda de arquivos = o botão de exportar
  é a rede de segurança da associação.
- **A sustentação depois da entrega é risco real, não hipótese.** Ninguém dentro
  da associação mantém tecnologia. Isso é argumento contra qualquer peça de
  infraestrutura que exija manutenção — e contra assinar plano pago que alguém
  precise renovar.

## O que o formulário reenquadra

O objetivo declarado da associação não é doação, é **geração de renda**:

> "Implantar o maior programa de geração de renda das comunidades atendidas para
> uma qualidade de vida mais independente."

O cadastro deixa de ser "lista de compras para doação" e passa a ser a **base de
um programa de capacitação e geração de renda**. Isso não muda nenhuma tabela
hoje, mas muda o discurso da apresentação — e sugere que, no futuro, campos de
ocupação e interesse em capacitação façam mais sentido que mais campos de
tamanho de roupa.

---

## Requisitos

Rastreabilidade: cada linha aponta para a origem e para a tela do Figma.

### Funcionais

| ID | Requisito | Origem | Telas | Situação |
|----|-----------|--------|-------|----------|
| RF-01 | Cadastrar família com seus membros numa única tela | Reunião 26/08 | 03 | Modelado |
| RF-02 | Buscar família pelo nome da responsável | Reunião 26/08 | 02 | Implementado |
| RF-03 | Contagem de roupa e calçado por comunidade | Documentos em Word | 04, 05 | Implementado |
| RF-04 | Registrar moradia e fontes de renda da família | Mensagem 25/08 | 03 | Modelado |
| RF-05 | Exportar dados para Excel | Reunião 26/08 + form (backup frágil) | 02, 04 | A fazer |
| RF-06 | Imprimir a lista por comunidade | Reunião 26/08 | 04 | A fazer |
| RF-07 | Ver as comunidades atendidas em mapa | Ideia do grupo | 01 | Implementado (API) |
| RF-08 | Relatório de situação das famílias para doador e prefeitura | Mensagem 25/08 | 04 | Implementado |
| RF-09 | Marcar cadastro como incompleto e voltar depois | Análise da lista manuscrita | 03 | Modelado |
| RF-10 | Cadastro de voluntários com disponibilidade | Formulário 31/08 | — | **Proposto, ver Q-04** |
| RF-11 | Anexar fotos a família, comunidade ou campanha | Formulário 31/08 | — | **Proposto, ver ADR-0008** |

### Não funcionais

| ID | Requisito | Origem | Situação |
|----|-----------|--------|----------|
| RNF-01 | Um único usuário com acesso | Reunião 26/08 | Implementado — **sob revisão, Q-01** |
| RNF-02 | Funcionar em celular e computador | Reunião 26/08 | Em andamento |
| RNF-03 | Senha com hash forte e sessão que expira | Boa prática + preocupação com golpes | Implementado |
| RNF-04 | Listas fechadas para os campos categóricos | Análise dos documentos atuais | Implementado |
| RNF-05 | Totais calculados, nunca digitados | Análise dos documentos atuais | Implementado |
| RNF-06 | Não exigir dado que a associação não tem (data de nascimento) | Análise da lista manuscrita | Implementado |
| RNF-07 | O sistema precisa sobreviver sem manutenção técnica interna | Formulário: suporte é externo | Guia as escolhas de infraestrutura |

### Fora de escopo, com motivo

| Item | Por que fica de fora |
|---|---|
| Gestão de projetos | Karla respondeu que **é fácil** saber o que está em andamento. Não é dor. |
| Controle de solicitações | "Muito poucas" solicitações. Não é dor. |
| Divulgação e redes sociais | Dificuldade é "maior alcance" — problema de marketing, não de software. |
| Estoque de materiais | Real, mas é outro sistema. Candidato à versão 2. |
| Biblioteca de fotos e vídeos | Ver [ADR-0008](decisoes/ADR-0008-fotos-e-videos.md). |
