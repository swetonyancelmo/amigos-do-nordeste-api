# ADR-0006 — Dois repositórios em vez de monorepo

**Data:** 31/08/2026 · **Situação:** aceita · **Substitui parte da** ADR-0001

## Decisão

`cadastro-familias-api` e `cadastro-familias-web` são repositórios
independentes, um por equipe.

## Motivo

Cada equipe fica dona do seu repositório: histórico limpo, PRs que não se
cruzam, CI que roda só o que interessa e permissões separadas. Para um grupo de
faculdade com duas equipes trabalhando em paralelo, isso reduz atrito no dia a
dia mais do que o monorepo reduziria.

## O custo, dito com todas as letras

O monorepo garantia uma coisa que agora precisa de disciplina: **o contrato
entre as duas pontas**. Antes, mudar um enum e não avisar quebrava o TypeScript
na hora. Agora não quebra nada — o erro só aparece quando alguém abre a tela.

Duas medidas seguram isso:

1. **`GET /api/metadados`** serve todas as listas fechadas com rótulo pronto. O
   frontend monta os `<select>` com o que vier da API, em vez de manter uma
   cópia. Lista copiada nos dois lados vira lista divergente em duas semanas.
2. **O Swagger em `/api/docs`** é a referência das rotas, e o checklist do PR
   pergunta explicitamente se a mudança afeta a outra ponta.

O frontend mantém só as *declarações de tipo* (`src/tipos/dominio.ts`), sem
valores nem rótulos — e o arquivo diz de onde ele veio.

## Onde ficam os documentos

ADRs, requisitos e a especificação ficam **no repositório da API**, porque é
onde o modelo de dados vive. O README do frontend aponta para cá. Não é
perfeito — é a escolha menos ruim sem criar um terceiro repositório só de
documentação.
