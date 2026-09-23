# Contexto para assistentes de IA — API

Backend do cadastro de famílias da Associação Amigos do Nordeste (Sertão do
Moxotó, PE). Trabalho semestral de faculdade. O frontend fica em **outro
repositório** (`cadastro-familias-web`, Next.js).

## Stack

Java 21 · Spring Boot 3.4 · Spring Data JPA · Spring Security · Flyway ·
PostgreSQL (Neon em produção) · springdoc-openapi · Maven

## Estado atual

Este repositório contém **apenas o módulo de autenticação**, de propósito: o
time não deve gastar tempo com Spring Security. Todo o domínio (família, pessoa,
comunidade, município, fonte de renda, relatórios) ainda vai ser construído
pelas pessoas do grupo — o modelo está em `docs/` e as tarefas no Kanban.

Ao ajudar aqui, **não construa o domínio inteiro de uma vez.** Faça a tarefa
pedida, no tamanho pedido.

## Regras

1. **Português em tudo**: classes, tabelas, colunas, rotas, commits, comentários.
   Colunas `snake_case`, campos Java `camelCase`.
2. **Nada que possa ser calculado é digitado.** Não crie colunas de total
   (total de pessoas, quantos até 12, quantos com 60+) — são consulta.
3. **Listas fechadas são enums**, e devem ser servidas por um endpoint de
   metadados quando o domínio for construído. Nunca texto livre onde deveria
   haver lista.
4. **`ddl-auto: none` sempre.** Schema muda por migração Flyway nova; nunca
   edite uma migração existente.
5. **O cadastro de usuário existe, mas é autenticado** (`POST /api/usuarios`).
   Nunca o torne público. A primeira conta vem do perfil `criar-usuario`.
6. **Trancado por padrão**: `anyRequest().hasRole("ADMIN")`; abrir rota só
   acrescentando à lista de `permitAll` em `SegurancaConfig`. O token do
   aparelho da agente (`ROLE_AGENTE`) abre **só** `POST /api/pre-cadastros`,
   listado explicitamente ali — nunca dê a ele mais que isso.
7. **Data de nascimento é opcional**; existe `idadeEstimada` + `idadeEstimadaEm`.
   Nunca torne a data obrigatória.
8. **Mapa é por comunidade**, nunca por família.
9. **Entidade não vira resposta de API** — sempre um `record` de DTO.
10. Dado real de família não entra em migração de exemplo, teste ou exemplo.

## Antes de mudar arquitetura ou escopo

Leia `docs/decisoes/` e `docs/requisitos.md`. Há duas questões bloqueantes em
aberto vindas da elicitação (Q-01 acesso único vs. mais de 100 colaboradores;
Q-02 cadastro vs. biblioteca de fotos). Se a mudança contraria uma ADR, atualize
a ADR no mesmo PR — não a ignore em silêncio.

## Comandos

```bash
mvn spring-boot:run
mvn verify
mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario
```
