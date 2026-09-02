# ADR-0007 — Java e Spring Boot no backend

**Data:** 31/08/2026 · **Situação:** aceita · **Substitui a escolha de stack da** ADR-0001

## Decisão

A API passa a ser **Java 21 + Spring Boot 3.4 + JPA/Hibernate + Flyway**, no
lugar de NestJS + TypeORM. O frontend continua em Next.js.

## Motivo

É a stack que a equipe de backend domina. Num projeto de um semestre, com prazo
fixo e sem tempo para aprender ferramenta nova, **a familiaridade da equipe vale
mais que qualquer característica técnica** das duas opções — e as duas dão conta
deste sistema com folga.

## O que mudou junto

| Antes | Agora | Observação |
|---|---|---|
| TypeORM migrations | **Flyway** (`V1__*.sql`) | SQL puro e versionado. Mesma regra: migração que já rodou nunca se edita. |
| `synchronize: false` | `spring.jpa.hibernate.ddl-auto: none` | Mesma decisão, nome diferente. |
| argon2 (npm) | `Argon2PasswordEncoder` + BouncyCastle | Mesmo algoritmo, argon2id. |
| Guard global do Nest | `SecurityFilterChain` com tudo autenticado e lista curta de rotas públicas | Trancado por padrão, aberto por exceção. |
| `text[]` para abastecimento de água | Tabela `familia_abastecimento_agua` | Array nativo em JPA é possível mas frágil. A tabela auxiliar é chata e funciona sempre. |
| `class-validator` | Bean Validation (`@Valid`, `@NotBlank`) | Equivalente. |
| Swagger do Nest | springdoc-openapi | Continua sendo o contrato com o frontend. |

## O que **não** mudou, e é o que importa

O modelo de dados, as regras e as decisões anteriores continuam valendo linha por
linha: totais calculados e nunca digitados, data de nascimento opcional com
idade estimada datada, listas fechadas servidas por `/api/metadados`, mapa por
comunidade, sem rota de cadastro de usuário.

Isso é o sinal de que as decisões estavam no lugar certo: **eram decisões de
domínio, não de framework.** Trocar a stack inteira não mexeu em nenhuma delas.
Vale dizer isso na apresentação.

## Consequência

O trabalho já feito em NestJS foi descartado. Custou pouco porque era esqueleto,
e a troca aconteceu antes da implementação das telas — o que reforça a ordem de
fazer modelagem e decisões antes de código.
