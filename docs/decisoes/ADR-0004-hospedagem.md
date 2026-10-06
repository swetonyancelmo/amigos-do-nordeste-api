# ADR-0004 — Hospedagem e o problema do "primeiro acesso do dia"

**Data:** 27/08/2026 · **Situação:** aceita

## O que foi verificado

Havia a suspeita de que o **banco** demoraria a acordar. Está errado:

- **Neon** suspende o compute após 5 minutos de inatividade (fixo no plano
  gratuito) e volta **em algumas centenas de milissegundos**. Na prática, a
  usuária não percebe.
- O problema real é o **host da API**. No plano gratuito do Render, serviços web
  hibernam após 15 minutos de inatividade e levam **cerca de um minuto** para
  subir. Um minuto de tela branca faz qualquer usuário concluir que o sistema
  não funciona.

## Decisão

1. Publicar a API em plataforma **sem hibernação forçada** no plano gratuito, ou
   com partida rápida (Fly.io com auto-start, ou funções serverless na Vercel).
2. Usar sempre a connection string **com pooler** do Neon (a que contém
   `-pooler`), porque em ambiente serverless cada invocação abre conexão.
3. Manter `GET /api/saude` e agendar um ping matinal, para que a primeira
   requisição do dia da associação nunca seja a que espera o servidor subir.
4. **Não assinar plano pago durante o semestre.** Um plano pago que vence depois
   da entrega, sem ninguém para renovar, é pior que um gratuito — ver o risco de
   sustentação na especificação.

Se ainda assim o serviço acabar num host que hiberna, aí sim vale avisar a
associação de que o primeiro acesso do dia demora — mas isso é o último recurso,
não o plano.

## Adendo (01/10/2026): o painel fala com a API pelo proxy do Next

O cookie de renovação é `SameSite=Lax` com caminho `/api/auth` (ADR-0002). Com
o painel e a API em sites diferentes (por exemplo `*.vercel.app` e `*.fly.dev`),
o navegador não manda esse cookie no `fetch` de `/api/auth/renovar`: cada
recarga da página deslogaria a usuária. `SameSite=None` também não resolve, por
causa do bloqueio de cookie de terceiros do Safari e do Chrome.

Decisão: o web chama sempre `/api/...` na **própria origem**, e o `rewrites`
do `next.config.mjs` repassa para a API (`API_URL`, variável do servidor do
Next). Para o navegador, painel e API são o mesmo site; o cookie viaja, o
CORS deixa de entrar no caminho do painel e a API pode ficar em qualquer host.
O app do celular continua falando direto com a API (`extra.apiUrl`).

## Adendo (05/10/2026): primeiro deploy no Render gratuito

A API foi para o **Render, plano gratuito**, mesmo hibernando: roda o
container Java sem custo e sem plano pago que vença depois da entrega
(decisão 4). O que isso custou, e como foi contornado (passo a passo em
[`docs/DEPLOY.md`](../DEPLOY.md)):

- **Hibernação** (15 min parado, ~1 min para acordar): ping externo em
  `GET /api/saude` a cada 10 minutos, **só das 6h às 22h**. O Render dá 750
  horas de instância por mês por conta; 24 h × 31 dias = 744 h deixaria só 6 h
  de folga, e a janela gasta ~500 h. Isso é paliativo, não solução.
- **512 MB e 0,1 CPU**: imagem com JRE e flags de heap pequeno (`Dockerfile`),
  medidas: pico de 481 MB com o fluxo completo. O login usa 64 MiB de heap por
  vez (argon2id), então quatro logins ao mesmo tempo derrubam o processo, que
  reinicia sozinho.
- **Pooler do Neon** (decisão 2): mantido. A string do Neon é colada como vem em
  `DATABASE_URL`; a API converte para JDBC e manda o Flyway pelo host direto
  (`config/ConversorUrlBanco`). O pool de conexões fecha tudo depois de 2 min
  ocioso, para não manter o Neon acordado (100 CU-hora/mês no gratuito).
- **Região**: Render em Virginia e Neon em AWS us-east-1, um ao lado do outro.
  O Render não tem região na América do Sul.

A saída definitiva, se a associação passar a usar o sistema depois do
semestre, é uma máquina que não hiberna: o `Dockerfile` não depende do Render
e roda igual numa VM.
