# ADR-0002 — Autenticação própria, e por que não existe tela de cadastro

**Data:** 27/08/2026 · **Situação:** aceita, **sob revisão** (ver abaixo)

## Contexto

A escolha inicial era Supabase, que entrega Postgres **e** autenticação prontos.
Com a decisão de ter um backend NestJS (ADR-0001), o banco passou a ser Neon —
que é só Postgres. O que se perde na troca não é o banco: é o **Auth**.

A associação foi explícita: **uma pessoa cadastra, uma pessoa tem acesso.**

## Sob revisão desde 31/08/2026

O formulário de elicitação respondido pela coordenadora de projeto diz que
**"mais de 100"** pessoas colaboram com a associação, e que existem informações
que **deveriam ter acesso restrito** — o que só faz sentido se mais de uma
pessoa acessa o sistema.

Isso contradiz a decisão da dona ("uma pessoa cadastra, uma pessoa tem acesso").
As duas podem ser verdade: colaborar com a associação não é operar o sistema.
Mas enquanto a questão Q-01 de `docs/requisitos.md` não for respondida, esta ADR
está sob revisão.

**Seguimos com acesso único**, porque foi o que a decisora pediu, e porque a
estrutura atual aceita perfis depois sem reescrita: basta uma coluna de papel na
tabela `usuario` e regras de autorização nas rotas. Nada do que está construído
precisaria ser jogado fora.

## Decisão

Autenticação implementada na própria API, com Postgres (Neon) como banco.

Com uma usuária só, a superfície de auth é minúscula: entrar, renovar, sair,
trocar senha. Não há fluxo de inscrição, verificação de e-mail, login social nem
papéis. É o tamanho certo para a equipe de backend construir **corretamente** e
de fato aprender — e tira um fornecedor do diagrama.

### Não existe rota de cadastro

`POST /api/auth/register` não existe e não deve ser criada. Com uma usuária, uma
rota pública de registro é um buraco de segurança sem nenhum benefício. A conta
nasce de `pnpm usuario:criar`, rodado uma vez na instalação.

### Regras que não são negociáveis

| Regra | Motivo |
|---|---|
| Hash **argon2id** | Senha nunca em texto. Hash rápido (MD5/SHA) é exatamente o que não se quer. |
| Access token de 15 min, **em memória** no frontend | Token parado em `localStorage` é o que um XSS leva embora. |
| Refresh em cookie `httpOnly` + `Secure` + `SameSite=Lax` | O JavaScript da página não consegue ler. |
| `SecurityFilterChain` com `anyRequest().authenticated()` e lista curta de rotas públicas | Trancado por padrão, aberto por exceção — nunca o contrário. |
| Rate limit de 5/min no login | Com uma usuária, força bruta é o único vetor real. |
| Mesma mensagem para e-mail errado e senha errada | Dizer qual dos dois falhou entrega metade da resposta. |
| Bean Validation com `@Valid` em todo corpo de requisição | Campo que não está no DTO não entra no banco. |

## Recuperação de senha

Não há serviço de e-mail no projeto. Se a senha for perdida, o grupo roda o perfil `criar-usuario` de novo, que a redefine. **Isso precisa estar no manual de
entrega**, junto com o risco: uma pessoa só com acesso também é um ponto único
de falha. A mitigação combinada é o botão de exportar tudo, usado de tempos em
tempos.

## Alternativa considerada

Um provedor de identidade externo (Keycloak, Auth0, Supabase Auth), com a API só
validando o token. Menos código e recuperação de senha de graça — mas mantém um
fornecedor a mais e tira da equipe de backend justamente a parte mais didática.
Se o prazo apertar, é o plano B mais barato de executar.

O formulário reforça a decisão por outro lado: a preocupação de segurança
declarada pela associação é **"golpes com nossos dados e contas"**. Menos contas
em menos serviços é menos superfície para golpe.
