# ADR-0001 — Backend NestJS separado do frontend

**Data:** 27/08/2026 · **Situação:** aceita · **Complementada pela** [ADR-0006](ADR-0006-dois-repositorios.md)

## Contexto

O sistema tem **uma usuária** e um volume pequeno (a associação divulga mais de
2.500 famílias atendidas). Do ponto de vista puramente técnico, um app único em
Next.js com rotas de servidor resolveria, e um backend separado dobra o deploy,
o CORS e a superfície de manutenção.

## Decisão

Mesmo assim, **backend separado em NestJS**.

O motivo não é técnico, é organizacional e pedagógico: o grupo se dividiu em
duas equipes, uma de frontend e outra de backend. Uma arquitetura que não dá
trabalho real para metade do grupo é uma arquitetura errada para este projeto.
A disciplina também pede a separação.

Registrar isso importa: a decisão foi tomada **com consciência do custo**, não
por inércia. Quem ler o repositório depois precisa saber disso.

## Consequências

- Duas aplicações para publicar, dois `.env`, CORS para configurar.
- Contrato explícito entre as equipes. Com a decisão por dois repositórios
  (ADR-0006), quem segura esse contrato é `GET /api/metadados` mais o Swagger —
  não existe mais um pacote compartilhado para o compilador conferir.
- O plano gratuito de alguns hosts hiberna o serviço; ver ADR-0004.
