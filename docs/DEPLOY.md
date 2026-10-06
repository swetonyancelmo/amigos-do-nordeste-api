# Deploy: primeira publicação do sistema

Roteiro para publicar o sistema **sozinho, do zero**, sem nunca ter usado Neon,
Render ou Vercel. Siga na ordem: cada passo usa um valor que o anterior gerou.
Reserve umas **2 horas**. Nenhum passo pede cartão de crédito; se alguma tela
pedir, **pare** e leia a observação do passo.

| Peça | Onde fica | Plano |
|---|---|---|
| Banco (PostgreSQL) | **Neon** | gratuito |
| API (este repositório) | **Render** | gratuito: 512 MB, 0,1 CPU, hiberna |
| Painel web (`amigos-do-nordeste-web`) | **Vercel** | gratuito (Hobby) |
| Despertador da API | **cron-job.org** | gratuito |

Onde este documento diz **⚠ conferir**, é um nome de botão ou de tela que não
deu para confirmar na documentação oficial. A ação está certa; o texto do
botão pode estar um pouco diferente.

---

## Antes de começar

**Você precisa de:**

- uma conta no GitHub com acesso aos repositórios `swetonyancelmo/amigos-do-nordeste-api`
  e `swetonyancelmo/amigos-do-nordeste-web` (as três plataformas entram com
  "Continuar com GitHub");
- o **e-mail** com que a usuária da associação vai entrar no sistema;
- um bloco de notas **no seu computador** (não no grupo, não na nuvem) para
  anotar os valores abaixo. Vários são senha.

**Vá preenchendo esta tabela no seu bloco de notas:**

| | O quê | Sai do passo | Exemplo do formato |
|---|---|---|---|
| **A** | String de conexão do Neon | 1 | `postgresql://neondb_owner:npg_…@ep-…-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require` |
| **B** | URL da API no Render | 2 | `https://cadastro-familias-api.onrender.com` |
| **C** | E-mail da usuária | você já tem | |
| **D** | Senha inicial da usuária | 4 | 16 letras e números |
| **E** | URL do painel na Vercel | 5 | `https://amigos-do-nordeste-web.vercel.app` |

### Por que esta ordem (a "dependência circular")

Parece haver um círculo: a API quer saber a URL do painel (CORS) e o painel quer
saber a URL da API. Ele se desfaz assim:

- **o painel precisa da URL da API para ser construído** (`API_URL`, lida no
  build). Então a API vem **antes**;
- **a API não precisa da URL do painel para funcionar.** O painel não chama a
  API direto do navegador: chama `/api/...` no próprio endereço da Vercel, e o
  servidor do Next repassa para o Render (ADR-0004, adendo de 01/10). Para o
  navegador, painel e API são o mesmo site, então o CORS nem entra no caminho.
  `APP_CORS_ORIGENS` vale só para quem chamar a API direto de outra página.

Por isso: API com um `APP_CORS_ORIGENS` **provisório** (passo 2) → painel com a
URL da API (passo 5) → volta à API e põe a URL real do painel (passo 6), para
deixar a configuração fechada.

```
 navegador ──► Vercel (painel)  ──/api/... repassado──►  Render (API)  ──►  Neon (banco)
                 API_URL = B                              DATABASE_URL = A
 celular da agente (app) ────────────── direto ─────────►  Render (API)
 cron-job.org ── GET /api/saude a cada 10 min ──────────►  Render (API)
```

---

## Passo 0: o código de deploy precisa estar na `main`

O Render constrói a partir da branch `main` do repositório da API. O
`Dockerfile`, o `render.yaml` e o perfil `prod` precisam estar lá.

1. No GitHub, abra o repositório `amigos-do-nordeste-api` > **Pull requests**.
2. O PR da branch `chore/preparar-deploy` precisa estar **merged** e com o CI
   verde (✓ verde ao lado do último commit da `main`).

**Deu certo quando:** na página inicial do repositório, na `main`, aparecem os
arquivos `Dockerfile` e `render.yaml` na raiz.

---

## Passo 1: Neon (o banco)

1. Entre em <https://neon.com> e crie a conta (dá para entrar com o GitHub). O
   plano gratuito não deve pedir cartão. **Se pedir, pare**: não é este plano.
2. Crie um projeto (**New Project**, ⚠ conferir):
   - **Project name:** `cadastro-familias`
   - **Postgres version:** `16`
   - **Region:** **AWS US East 1 (N. Virginia)**. **Não escolha São Paulo.**
     A API vai ficar no Render em Virginia (o Render não tem região na América
     do Sul) e cada tela faz várias consultas ao banco; com o banco em São
     Paulo, cada uma atravessaria o continente. **A região não muda depois**:
     se errar, apague o projeto e crie outro.
3. Abra o projeto e clique em **Connect** (no topo do painel do projeto). Abre
   a janela **Connect to your branch**.
4. Na janela:
   - deixe **Connection pooling ligado** (é o padrão). Confira: o endereço
     copiado precisa ter **`-pooler`** no meio do host;
   - copie a **string de conexão** inteira (botão de copiar ao lado dela).
     Confira que a senha veio de verdade no texto copiado e não `****`. Se vier
     mascarada, procure a opção de mostrar a senha antes de copiar (⚠ conferir).
5. Cole no bloco de notas como **A**.

### A string do Neon: cole do jeito que ela vem

Antes, o Spring só aceitava a URL no formato JDBC, com usuário e senha em
variáveis separadas, e colar a do Neon dava um erro que não explicava nada. A
API agora faz essa conversão sozinha (`config/ConversorUrlBanco`). **Você cola a
da esquerda; a API usa a da direita. Não converta à mão.**

| Você cola em `DATABASE_URL` (exatamente como o Neon mostra) | O que a API passa a usar sozinha |
|---|---|
| `postgresql://neondb_owner:SUA_SENHA@ep-exemplo-000000-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require` | URL: `jdbc:postgresql://ep-exemplo-000000-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require` |
| | usuário: `neondb_owner` |
| | senha: `SUA_SENHA` |
| | Flyway (migrações): `jdbc:postgresql://ep-exemplo-000000.us-east-1.aws.neon.tech/neondb?…` (o mesmo host **sem** `-pooler`) |

(`SUA_SENHA` e `ep-exemplo-000000` são marcadores: no seu, vêm a senha e o host reais.) Regras para não errar:

- a string começa com **`postgresql://`**. Se o que você copiou começa com
  `psql '…'`, tudo bem, a API tira o `psql` e as aspas; mas o ideal é a
  string pura;
- **não** coloque espaço, quebra de linha, `DATABASE_URL=` nem aspas extras
  dentro do campo do Render;
- **não** preencha `DATABASE_USUARIO` nem `DATABASE_SENHA`: com a string do
  Neon, a API os tira de dentro dela.

<details>
<summary>Plano B, só se preferir montar à mão (não é necessário)</summary>

| Variável | Valor |
|---|---|
| `DATABASE_URL` | `jdbc:postgresql://ep-exemplo-000000-pooler.us-east-1.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USUARIO` | `neondb_owner` |
| `DATABASE_SENHA` | `SUA_SENHA` |

Ou seja: troque `postgresql://usuario:senha@` por `jdbc:postgresql://` e leve
usuário e senha para as duas variáveis. Se esquecer as duas variáveis, o erro é
`password authentication failed for user`.
</details>

**Deu certo quando:** você tem a string **A** anotada, começando com
`postgresql://`, com `-pooler` no host e `sslmode=require` no fim. O banco
ainda está vazio; quem cria as tabelas é a API, no próximo passo.

---

## Passo 2: Render (a API)

O `render.yaml` na raiz do repositório já descreve o serviço (Docker, plano
gratuito, região Virginia, health check, variáveis). Você só preenche os
valores secretos.

1. Entre em <https://render.com> com a conta do GitHub. Na primeira vez, o
   Render pede para instalar o app dele no GitHub: dê acesso **ao repositório
   `amigos-do-nordeste-api`**.
2. No painel (<https://dashboard.render.com>): **New > Blueprint**.
3. Escolha o repositório `amigos-do-nordeste-api` e clique em **Connect**.
4. **Blueprint Name:** `cadastro-familias`. **Branch:** `main`. O caminho do
   arquivo fica o padrão (`render.yaml`).
5. O Render mostra o serviço `cadastro-familias-api` e **pede os valores das
   variáveis marcadas para preenchimento manual**. Preencha:

   | Variável | Valor |
   |---|---|
   | `DATABASE_URL` | **A**, a string do Neon, exatamente como copiou |
   | `APP_CORS_ORIGENS` | `http://localhost:3000` (**provisório**; o passo 6 corrige) |
   | `APP_USUARIO_INICIAL_EMAIL` | **C**, o e-mail da usuária, minúsculo |

   As outras já vêm do `render.yaml` e **não precisam de nada**:

   | Variável | Valor | Por quê |
   |---|---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` | liga `application-prod.yml` |
   | `APP_JWT_SEGREDO` | gerado pelo Render | 256 bits aleatórios; ninguém precisa ver |
   | `APP_COOKIE_SEGURO` | `true` | cookie de sessão só em HTTPS |
   | `SERVER_FORWARD_HEADERS_STRATEGY` | `native` | IP real do visitante atrás do proxy do Render |
   | `APP_USUARIO_INICIAL_NOME` | `Associação Amigos do Nordeste` | nome da conta |

   **Não crie `PORT`**: o Render a define sozinho (10000) e a API a lê.
6. Clique em **Deploy Blueprint**.

> **Se o Render pedir cartão no fluxo de Blueprint:** cancele e crie o serviço
> à mão: **New > Web Service** > repositório `amigos-do-nordeste-api` >
> **Language/Runtime: Docker** > **Region: Virginia** > **Instance Type: Free**.
> Em **Environment Variables**, crie as oito variáveis das duas tabelas acima.
> Para `APP_JWT_SEGREDO`, gere o valor num terminal com
> `openssl rand -base64 48` e cole (ele não vai para lugar nenhum além do
> Render). Em **Advanced** (⚠ conferir), **Health Check Path:** `/api/saude`.

**O primeiro build demora** (de 5 a 15 minutos): o Docker baixa o Maven e
todas as dependências do projeto. Acompanhe em **Logs**, no serviço.

**Deu certo quando:**

- o serviço aparece como **Live** (⚠ conferir o texto exato do selo) e o
  último deploy não está em vermelho;
- no topo da página do serviço aparece a URL `https://….onrender.com`.
  **Anote como B.** Ela pode ter um sufixo aleatório se o nome já existia.

---

## Passo 3: conferir que a API subiu

1. Abra no navegador **`B/api/saude`** (ex.:
   `https://cadastro-familias-api.onrender.com/api/saude`). Tem que aparecer:

   ```json
   {"ok":true,"em":"2026-10-06T09:41:03.123456Z"}
   ```

   (a data e a hora mudam). Se demorar perto de um minuto na primeira vez, é a
   hibernação; espere.
2. Abra **`B/api/familias`**. Tem que dar **erro 401** (página em branco ou
   "não autorizado"). Isso é **bom**: a rota é trancada e nada vaza sem login.
3. Abra **`B/swagger-ui.html`**: a documentação da API abre, com o título
   "Cadastro de Famílias — Amigos do Nordeste".
4. No Render, abra **Logs** do serviço e procure estas linhas, na ordem:

   ```
   The following 1 profile is active: "prod"
   DATABASE_URL no formato postgresql:// convertida para JDBC (host ep-…-pooler.us-east-1.aws.neon.tech).
   Flyway migra pelo host direto do Neon (ep-….us-east-1.aws.neon.tech), sem o pooler.
   Successfully applied 14 migrations to schema "public", now at version v14
   Tomcat started on port 10000 (http) with context path '/'
   Started Startup in … seconds
   ```

   A linha `Successfully applied 14 migrations` é a prova de que o Flyway criou
   as tabelas no Neon. **Só aparece no primeiro deploy.** Nos seguintes, a linha
   é `Schema "public" is up to date. No migration necessary.`, que também está
   certa. Se uma migração nova entrar no futuro (V15…), aparece
   `Successfully applied 1 migration`.

Nenhuma senha aparece nessas linhas: a API registra o host do banco, nunca a
senha.

---

## Passo 4: criar a conta da usuária (o passo que trava tudo se esquecido)

O sistema **não tem tela de cadastro**, de propósito (ADR-0002). Sem este passo
a API sobe, o painel abre, e **ninguém consegue entrar**: todo login responde
"credenciais inválidas" (401).

A conta nasce do perfil `criar-usuario`: ao subir com ele, a API cria a conta
com o e-mail de `APP_USUARIO_INICIAL_EMAIL`, sorteia uma senha e a imprime **no
log**. O plano gratuito do Render não tem terminal (shell) nem "one-off job",
então o caminho é ligar o perfil por **um deploy** e desligar em seguida.

### 4.1 Ligar

1. No Render, no serviço `cadastro-familias-api` > **Environment**.
2. Edite `SPRING_PROFILES_ACTIVE` de `prod` para:

   ```
   prod,criar-usuario
   ```

   (sem espaço, com vírgula).
3. Salve escolhendo a opção que **salva e faz deploy** (o Render oferece
   salvar com ou sem novo deploy; ⚠ conferir o nome do botão). Se só salvou,
   vá em **Manual Deploy > Deploy latest commit**.
4. Abra **Logs** e espere o deploy. Logo depois de `Started Startup in …`,
   aparece:

   ```
   Usuária criada: email-da-usuaria@exemplo.org
   --------------------------------------------------------
     senha: Xy7…(16 caracteres)
   --------------------------------------------------------
   Anote agora — ela não será mostrada de novo.
   ```

5. Copie a senha para o bloco de notas como **D**. Confira o e-mail da linha
   `Usuária criada`.

### 4.2 Desligar: agora, sem pausa

6. Volte a **Environment**, ponha `SPRING_PROFILES_ACTIVE` de volta para
   **`prod`** e salve **com deploy**.
7. Nos **Logs** do novo deploy, confira `The following 1 profile is active: "prod"`
   e que **não** aparece `Usuária criada` nem `Senha redefinida`.

**Por que não pode esquecer ligado:** o perfil roda **toda vez que a API
sobe**, e no plano gratuito ela sobe toda vez que acorda da hibernação. Cada
subida sortearia **uma senha nova** e a usuária não entraria mais com a que
você anotou.

### 4.3 Tirar a senha do log

A senha ficou nos logs do Render (só quem entra na conta do Render vê). Troque
por uma que só a usuária conheça, de preferência **junto com ela**. O painel
ainda não tem a tela (`/perfil` é esqueleto), então pelo Swagger:

1. Abra **`B/swagger-ui.html`**.
2. Em **auth**, abra `POST /api/auth/login` > **Try it out**, cole no corpo:
   ```json
   {"email": "C", "senha": "D"}
   ```
   e **Execute**. A resposta 200 traz `"accessToken": "eyJ…"`. Copie o valor,
   sem as aspas.
3. No topo, clique em **Authorize**, cole o token, **Authorize** e **Close**.
4. Abra `POST /api/auth/trocar-senha` > **Try it out**:
   ```json
   {"senhaAtual": "D", "senhaNova": "uma senha com pelo menos 10 caracteres"}
   ```
   **Execute**. Resposta esperada: `200` com `{"ok": true}`.

A senha nova vai para a usuária por um canal seguro (pessoalmente, ou ligação),
**nunca no grupo**.

**Perdeu a senha depois?** É o mesmo 4.1 e 4.2: o perfil **redefine** a senha
de quem já existe (o log diz `Senha redefinida para …`). É a recuperação de
senha do projeto, já que não há e-mail.

> **Alternativa sem a senha passar pelo log do Render** (para quem tem JDK 21 e
> Maven): rode o perfil na sua máquina apontando para o Neon. A senha só
> aparece no seu terminal.
>
> ```bash
> cd cadastro-familias-api
> mvn -q package -DskipTests
> SPRING_PROFILES_ACTIVE=prod,criar-usuario \
> DATABASE_URL='A' \
> APP_JWT_SEGREDO="$(openssl rand -base64 48)" \
> APP_CORS_ORIGENS=http://localhost:3000 \
> APP_USUARIO_INICIAL_EMAIL='C' \
> java -jar target/cadastro-familias-api-0.1.0.jar
> ```
>
> Espere a senha aparecer, anote, e pare com **Ctrl+C**. O segredo do JWT
> sorteado aqui é descartado; não importa, ele não fica gravado no banco.

**Deu certo quando:** o login do passo 4.3 respondeu 200 com um `accessToken`.

---

## Passo 5: Vercel (o painel)

1. Entre em <https://vercel.com> com o GitHub (plano **Hobby**, gratuito) e dê
   acesso ao repositório `amigos-do-nordeste-web`.
2. **Add New… > Project** (⚠ conferir) > **Import** no repositório
   `amigos-do-nordeste-web`.
3. Na tela de configuração:
   - **Framework Preset:** Next.js (a Vercel detecta sozinha);
   - **Root Directory:** `./` (o padrão);
   - comandos de build e instalação: deixe os padrões (ela detecta o `pnpm`
     pelo `pnpm-lock.yaml`);
   - **Environment Variables:** crie **uma** variável:

     | Name | Value |
     |---|---|
     | `API_URL` | **B**, ex. `https://cadastro-familias-api.onrender.com` |

     **Sem `/api` no fim e sem barra no fim.** O painel já acrescenta `/api/…`;
     com `/api` no valor, toda chamada viraria `/api/api/…` e daria 404.
     O nome é `API_URL` mesmo, **não** `NEXT_PUBLIC_API_URL`: quem lê é o
     servidor do Next, não o navegador.
4. **Deploy**. O build leva de 1 a 3 minutos.

**Deu certo quando:** o deploy termina como **Ready** e a Vercel mostra o
endereço de produção do projeto (algo como
`https://amigos-do-nordeste-web.vercel.app`, em **Domains**). **Anote como E.**
Use o domínio de produção, não os endereços longos com hash, que são de cada
deploy.

**Importante:** `API_URL` é lida **no build**. Se um dia a URL da API mudar,
troque a variável em **Settings > Environment Variables** e faça **Redeploy**;
só salvar não muda o site que já está no ar.

---

## Passo 6: voltar ao Render e fechar o CORS

1. Render > serviço `cadastro-familias-api` > **Environment**.
2. Edite `APP_CORS_ORIGENS` para **E**, **sem barra no fim**:

   ```
   https://amigos-do-nordeste-web.vercel.app
   ```

   Mais de uma origem: separe por vírgula, ex.
   `https://amigos-do-nordeste-web.vercel.app,https://outro-endereco.vercel.app`.
   **Curinga não funciona** (`https://*.vercel.app` é ignorado). Os previews da
   Vercel não precisam estar na lista: eles também passam pelo proxy do Next.
3. Salve **com deploy**.

**Deu certo quando:** o deploy fica Live de novo e `B/api/saude` responde.
(Lembre: o painel funciona mesmo antes deste passo, porque não usa CORS. Este
passo tira o `localhost` provisório da configuração de produção.)

---

## Passo 7: teste ponta a ponta

Use **só dados inventados**. Nada de nome, CPF ou telefone de família real,
nem em teste.

1. Abra **E**. Aparece a tela **Entrar**.
2. Entre com **C** e a senha do passo 4. Aparece o **Início**.
   - Se a API estava dormindo, o primeiro login pode levar até uns 2 minutos ou
     falhar com "Não foi possível falar com o servidor". Espere 1 minuto e
     tente de novo.
3. **Comunidades > Nova comunidade.** Escolha **Estado** e **Município** (a
   lista vem do IBGE) e dê um nome. A comunidade não se apaga e aparece no app
   das agentes, então, se puder, use **uma comunidade real que a associação
   atende** (comunidade não é dado de família). **Salvar comunidade** → aparece
   "Comunidade cadastrada".
4. **Famílias > Nova família.** Escolha a comunidade, **Responsável:**
   `Família Teste Deploy`. **Salvar família** → aparece "Família cadastrada".
5. Volte a **Famílias**: `Família Teste Deploy` está na lista.
6. **Aperte F5.** Você tem que **continuar logado**. É o teste do cookie de
   renovação: se cair no login, veja "O F5 desloga" em "Se der errado".
7. Limpe o teste: família não se apaga, se inativa. No Swagger (passo 4.3:
   login + Authorize), `POST /api/familias/{id}/inativar`, com o `id` que
   aparece no endereço da ficha da família no painel (`/familias/<id>`).
   Resposta 2xx, e ela some da lista.

**Deu certo quando:** os passos 2, 3, 4, 5 e 6 funcionaram.

---

## Passo 8: o ping para a API não dormir

O plano gratuito do Render **hiberna a API depois de 15 minutos sem receber
requisição**, e ela leva **cerca de um minuto para acordar** (documentação do
Render). Para a usuária, um minuto de tela parada é sistema quebrado, e no dia
da apresentação também. O paliativo é um serviço externo chamando
`/api/saude` a cada 10 minutos. Essa rota é pública, responde sem consultar o
banco e não abre conexão nenhuma (conferido), então chamá-la para sempre não
custa nada ao Neon.

### A conta: por que não pingar 24 horas

O Render dá **750 horas de instância gratuita por mês, por conta (workspace)**,
somando todos os serviços gratuitos dela. Se acabar, **todos os serviços
gratuitos ficam suspensos até o mês seguinte** (documentação do Render,
"Deploy for Free").

| Cenário | Conta | Horas/mês (mês de 31 dias) | Sobra de 750 |
|---|---|---|---|
| Ping 24 h | 24 h × 31 | **744 h** | **6 h** |
| Ping das 6h às 22h | pings de 06:00 a 21:50; acordada até ~22:05 = 16,1 h × 31 | **~499 h** | **~251 h** |

Com 24 h, sobram 6 horas: qualquer outro serviço gratuito na mesma conta, ou um
segundo serviço de teste esquecido, estoura o limite e derruba o sistema no fim
do mês, talvez no dia da apresentação. A janela das **6h às 22h** cobre o
horário em que alguém usa o sistema e deixa ~250 h de folga para acessos fora
dela (cada acesso fora da janela acorda a API e a mantém de pé por ~15 minutos).

### Configurar no cron-job.org

1. Crie a conta em <https://cron-job.org> (gratuito, sem cartão) e confirme o
   e-mail.
2. **Create cronjob** (⚠ conferir).
3. Preencha:
   - **Title:** `Acordar API do cadastro`
   - **URL:** `B/api/saude` (ex. `https://cadastro-familias-api.onrender.com/api/saude`)
   - **Execution schedule:** personalizado (⚠ conferir o nome da opção):
     - minutos: `0, 10, 20, 30, 40, 50` (a cada 10 minutos);
     - horas: **6 a 21** (a última execução é 21:50; a API dorme ~22:05);
     - todos os dias, todos os meses, todos os dias da semana.

     Se a tela aceitar expressão cron, é: `*/10 6-21 * * *`
   - **Fuso horário:** `America/Recife` (ou `America/Sao_Paulo`, mesmo horário).
     Confira onde o cron-job.org guarda o fuso (no job ou nas configurações da
     conta, ⚠ conferir): com UTC, a janela ficaria 3 horas adiantada.
4. Salve.

**Duas coisas normais que parecem erro:**

- a execução das **06:00** costuma aparecer como **falha (timeout)**: o
  cron-job.org espera 30 segundos, e a API leva cerca de um minuto para
  acordar. Ela acorda mesmo assim, e a das 06:10 responde 200. Se o
  cron-job.org mandar e-mail a cada falha, desligue a notificação de falha ou
  ignore a das 06:00;
- o cron-job.org **desativa o job sozinho depois de mais de 25 falhas
  seguidas** (FAQ dele). Isso não acontece com uma falha por dia; acontece se a
  URL estiver errada ou a API fora do ar. Se o job aparecer desativado, corrija
  a causa e reative.

**Deu certo quando:** no histórico do job, as execuções depois das 06:00 mostram
**200**, e `B/api/saude` aberto à tarde responde na hora.

**No dia da apresentação:** se for antes das 6h ou depois das 22h, abra
`B/api/saude` dois minutos antes. Ou estenda a janela só naquele dia.

### Isto é paliativo, não solução

- O ping **não resolve os 512 MB**: a API cabe, mas sem folga (ver
  "Memória" abaixo).
- Ele **depende de um serviço de terceiro** que pode mudar as regras, sair do
  ar ou desativar o job.
- Fora da janela, o primeiro acesso continua esperando um minuto.
- O banco também tem cota: o Neon gratuito dá **100 CU-hora por mês por
  projeto** (cerca de 400 h de um compute de 0,25 CU) e dorme depois de 5
  minutos parado. A API fecha as conexões depois de 2 minutos ociosa para
  deixá-lo dormir. Na primeira semana, confira o uso em **Usage**/**Billing**
  no console do Neon (⚠ conferir o nome).

**A saída definitiva**, se a associação passar a usar o sistema de verdade
depois do semestre, é **uma máquina que não hiberna**: um plano pago do Render
ou uma VM pequena, com memória sobrando. Nada no projeto está amarrado ao
Render: o `Dockerfile` roda igual em qualquer máquina com Docker
(`docker build -t cadastro-api . && docker run --env-file .env -p 3333:3333 cadastro-api`).
Pela ADR-0004, plano pago só faz sentido com alguém da associação
responsável por renovar e pagar; plano que vence sem ninguém para renovar é
pior que o gratuito.

---

## Se der errado

Os logs da API ficam em Render > serviço > **Logs**. Os do painel, em Vercel >
projeto > **Deployments** > o deploy > **Logs** (⚠ conferir).

### A string do Neon no formato errado

| Sintoma no log do Render | Causa | Correção |
|---|---|---|
| `URL must start with 'jdbc'` | `DATABASE_URL` vazia, ausente, ou com algo que não começa com `postgresql://` (só o host, `https://…`, `DATABASE_URL=…` colado dentro do valor) | Cole de novo a string **A** pura, começando com `postgresql://`. |
| `password authentication failed for user` | Senha errada na string (copiada com `****`, ou senha do role trocada no Neon); ou URL JDBC montada à mão sem `DATABASE_USUARIO`/`DATABASE_SENHA` | Copie a string de novo no **Connect** do Neon, com a senha visível. |
| Erro de conexão citando o host, ou `The connection attempt failed` | Host cortado na cópia, ou espaço/quebra de linha no meio | Cole de novo, de uma vez, sem editar. |
| Não aparece a linha `DATABASE_URL no formato postgresql:// convertida para JDBC` | A string não foi reconhecida como do Neon | Confira o começo: `postgresql://` ou `postgres://`. |

### Falta de memória / o container reinicia sozinho

| Sintoma | O que é | O que fazer |
|---|---|---|
| Log termina com `Terminating due to java.lang.OutOfMemoryError: Java heap space` e o serviço reinicia | O heap encheu. A JVM sai de propósito (`-XX:+ExitOnOutOfMemoryError`) e o Render sobe outra. Causa mais provável: **vários logins ao mesmo tempo** (cada um usa 64 MiB; quatro simultâneos já estouram) | Nada a fazer na hora: em ~1 minuto volta. Se repetir sem ninguém usando, pode ser alguém martelando o login: veja a seção de segurança. |
| O serviço reinicia sem nenhum erro de Java no log, e os eventos do Render falam em memória excedida (⚠ conferir o texto) | O **total** (heap + fora do heap) passou de 512 MB e o sistema matou o processo | Baixe `-XX:MaxRAMPercentage` no `Dockerfile` de 50 para 45, faça PR e deploy. Custa: três logins simultâneos passam a estourar o heap (medido). |
| Deploy falha antes de terminar, com log cortado no meio do Spring | Subida lenta demais (0,1 CPU) ou memória | Rode **Manual Deploy** de novo. O Render espera até 15 minutos pelo health check. |

Como a memória foi medida: o jar com perfil `prod`, limitado a 512 MB, contra
um Postgres local, com 60 famílias, listagem, relatórios, exportação `.xlsx`,
Swagger e logins simultâneos:

| `MaxRAMPercentage` | Heap | Pico de memória do processo | 3 logins juntos |
|---|---|---|---|
| 45 | 230 MB | — | **estoura o heap** |
| **50 (escolhido)** | 256 MB | **481 MB** | passa |
| 55 | 282 MB | 503 MB (colado no limite) | passa |

Fora do heap a JVM usa ~190 MB (classes ~83 MB, símbolos 36 MB, código
compilado 18 MB…). As flags e o porquê de cada uma estão comentados no
`Dockerfile`.

### CORS bloqueando chamadas

**O painel não deveria passar por CORS** (ele chama a própria origem). Se o
console do navegador (F12 > Console) mostra
`blocked by CORS policy: No 'Access-Control-Allow-Origin' header`:

- veja a **URL da requisição bloqueada**. Se ela é `https://….onrender.com/…`,
  alguém está chamando a API **direto** do navegador, sem passar pelo proxy.
  No painel, é um rascunho que usa `fetch` cru em vez de `src/lib/api.ts`
  (o `CLAUDE.md` da raiz avisa que há branches assim). Corrija o código; não
  "abra" o CORS;
- se a chamada direta é legítima (outra página que precisa da API), ponha a
  origem dela em `APP_CORS_ORIGENS`: exata, com `https://`, **sem barra no
  fim**, separada por vírgula das outras, e salve com deploy. Origem fora da
  lista recebe **403** no preflight (conferido).

### Login responde 401 (a conta não existe)

Sintoma: no painel, "e-mail ou senha" errados mesmo digitando certo; no
Swagger, `POST /api/auth/login` responde **401**.

- o passo 4 não foi feito, ou foi feito com outro e-mail. Nos Logs do Render,
  procure `Usuária criada:` e confira o e-mail;
- a senha anotada não vale mais porque o perfil `criar-usuario` ficou ligado e
  a API reiniciou (cada subida sorteia outra). Confira que
  `SPRING_PROFILES_ACTIVE` é exatamente `prod`, e refaça 4.1 e 4.2;
- o e-mail foi digitado com espaço ou maiúscula diferente: o login ignora
  maiúsculas, mas confira o e-mail do log.

### O F5 desloga (o cookie de renovação não fica)

O access token vive só na memória da página; quem mantém a sessão depois do F5
é o cookie `httpOnly` `and_refresh`, com caminho `/api/auth`.

- F12 > **Application** (Chrome) ou **Armazenamento** (Firefox) > **Cookies** >
  o endereço **E**: depois do login tem que existir `and_refresh`;
- **não existe:** confira que você está em `https://` (a Vercel é sempre
  HTTPS). `APP_COOKIE_SEGURO=true` faz o cookie só viajar em HTTPS, e é o que
  se quer em produção; com `false` ele até funcionaria, mas viajaria também em
  HTTP. **Não troque para `false` em produção** para "resolver";
- **existe, mas o F5 desloga mesmo assim:** o login foi feito direto no domínio
  do Render (por exemplo, pelo Swagger) e não pelo painel. O cookie é do
  domínio que respondeu; o painel só enxerga o que veio pelo proxy dele. Entre
  pelo painel;
- **o painel chama `onrender.com` direto** (veja na aba **Network**): o cookie
  `SameSite=Lax` não viaja entre sites diferentes. É o mesmo caso do CORS
  acima: a chamada precisa passar por `src/lib/api.ts`;
- localmente, com a API em `http://localhost` e `APP_COOKIE_SEGURO=true`, o
  cookie também some. Local é `false` (`.env.example`).

### Outros

| Sintoma | Causa | Correção |
|---|---|---|
| Painel: "Não foi possível falar com o servidor" logo de manhã | API acordando (até ~1 min) | Espere e recarregue. Se for sempre, confira o job do cron-job.org e o fuso. |
| Painel: todas as chamadas dão 404 | `API_URL` com `/api` no fim, ou a URL errada | Corrija em Vercel > Settings > Environment Variables e faça **Redeploy**. |
| Painel continua chamando a API antiga depois de trocar `API_URL` | A variável é lida no build | **Redeploy** na Vercel. |
| Log: `Could not resolve placeholder 'APP_JWT_SEGREDO'` (ou `APP_CORS_ORIGENS`) | Variável ausente no Render | Crie a variável (passo 2) e faça deploy. |
| Log: `Defina APP_USUARIO_INICIAL_EMAIL antes de rodar o perfil criar-usuario` | `criar-usuario` ligado sem o e-mail | Crie `APP_USUARIO_INICIAL_EMAIL` e faça deploy. |
| Build na Vercel falha na instalação com erro de versão do pnpm | O lockfile foi gerado com um pnpm mais novo que o da Vercel | Veja a versão que o build usou no log e alinhe (campo `packageManager` no `package.json` do web), em PR no repositório web. |
| Serviços do Render suspensos no fim do mês | Acabaram as 750 horas gratuitas da conta | Reduza a janela do ping; apague serviços gratuitos de teste na mesma conta. Volta no dia 1º. |

---

## Segurança, curto e direto

- **O segredo do JWT é um por ambiente e nunca vai para o Git.** Em produção, o
  Render o gera (`generateValue` no `render.yaml`) e ninguém precisa vê-lo. O
  valor do `application.yml` é público; por isso o perfil `prod` se recusa a
  subir sem `APP_JWT_SEGREDO`.
- **Se um segredo vazar** (JWT, string do Neon, senha da usuária), **apagar o
  commit ou a mensagem não resolve**: o Git guarda histórico, clones e forks
  guardam cópias, e o GitHub indexa. É preciso **trocar o segredo**:
  - JWT: no Render, edite `APP_JWT_SEGREDO` com um valor novo
    (`openssl rand -base64 48`) e faça deploy. Todos os logins caem; a usuária
    entra de novo;
  - banco: no console do Neon, redefina a senha do role (⚠ conferir onde),
    copie a string nova e atualize `DATABASE_URL` no Render;
  - senha da usuária: passo 4.3, ou 4.1 e 4.2 se ninguém souber a atual.
- **Nenhum dado real de família em print, log, issue, teste ou mensagem de
  grupo.** O cadastro tem menores, renda e moradia, e a associação já disse
  temer golpes com os dados dela. Print do painel para o relatório da
  disciplina: só com os dados inventados do passo 7.
- **A senha da usuária não vai pelo grupo.** Pessoalmente ou por ligação, e
  trocada no primeiro uso.
- O Swagger (`/swagger-ui.html`) fica aberto em produção, como já era: ele
  mostra a forma das rotas, não os dados; toda rota de dado exige login.

---

## Referência rápida

### Variáveis da API em produção (Render)

| Variável | Valor | De onde sai |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` (`prod,criar-usuario` só no passo 4) | fixo |
| `DATABASE_URL` | string do Neon, como vem | Neon > Connect, pooling ligado |
| `APP_JWT_SEGREDO` | aleatório | gerado pelo Render |
| `APP_CORS_ORIGENS` | URL de produção da Vercel | Vercel > Domains |
| `APP_COOKIE_SEGURO` | `true` | fixo |
| `SERVER_FORWARD_HEADERS_STRATEGY` | `native` | fixo |
| `APP_USUARIO_INICIAL_EMAIL` | e-mail da usuária | associação |
| `APP_USUARIO_INICIAL_NOME` | `Associação Amigos do Nordeste` | fixo |
| `PORT` | **não defina** | o Render injeta (10000) |
| `DATABASE_USUARIO`, `DATABASE_SENHA`, `SPRING_FLYWAY_URL` | **não defina** | saem da `DATABASE_URL` |

### Variável do painel (Vercel)

| Variável | Valor |
|---|---|
| `API_URL` | URL do Render, sem `/api` e sem barra no fim |

### `spring.main.lazy-initialization`: por que fica desligada

Ela poupa memória e tempo de subida, criando cada componente só quando é usado
pela primeira vez. O custo é adiar para a primeira requisição o erro de
configuração de qualquer componente: o deploy fica verde, `/api/saude` responde
(ele não depende de quase nada), e quem descobre o problema é a usuária, no meio
do uso. E, num serviço que hiberna, o tempo que ela pouparia na subida só
mudaria de lugar para a primeira tela. Preferimos que o deploy quebre na hora.
