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
