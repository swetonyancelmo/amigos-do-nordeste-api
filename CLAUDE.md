# Contexto para assistentes de IA — API

Backend do cadastro de famílias da Associação Amigos do Nordeste (Sertão do
Moxotó, PE). Trabalho semestral de faculdade. Há dois outros repositórios que
consomem esta API: `cadastro-familias-web` (Next.js, painel da associação) e
`cadastro-familias-app` (Expo, pré-cadastro offline pela agente de saúde).

## Stack

Java 21 · Spring Boot 3.4 · Spring Data JPA · Spring Security · Flyway ·
PostgreSQL 16 (Neon em produção) · springdoc-openapi · jjwt · Maven (sem wrapper)

## Estado atual

Auth e boa parte do domínio já estão prontos. Cada pacote em
`src/main/java/br/org/amigosdonordeste/cadastro/` é um módulo:

| Pacote | Rotas | O que faz |
|---|---|---|
| `auth` | `/api/auth/{login,renovar,sair,trocar-senha}` | JWT de 15 min no corpo, renovação em cookie `httpOnly` |
| `usuario` | `/api/usuarios` | contas do painel (autenticado); `CriarUsuarioRunner` no perfil `criar-usuario` |
| `municipio` | `/api/municipios` | CRUD sem exclusão, `codigo_ibge` único |
| `comunidade` | `/api/comunidades`, `/api/comunidades/opcoes` | CRUD com lat/long; `/opcoes` é a lista enxuta da agente |
| `familia` | `/api/familias` | cria/edita família com pessoas e fontes de renda numa chamada; lista paginada com filtros; inativar/reativar |
| `pessoa` | `/api/pessoas`, `/api/familias/{id}/pessoas` | lista com filtros, ficha, inclui, edita, remove |
| `fonterenda` | — (dentro de família) | entidade e enums de renda |
| `metadados` | `/api/metadados` (público) | todas as listas fechadas com rótulo |
| `relatorio` | `/api/relatorios/{necessidades,situacao}` | roupa/calçado por tamanho; indicadores de situação |
| `agente` | `/api/agentes/ativar` (público, com limite por IP) | troca código de convite por token do aparelho |
| `precadastro` | `/api/pre-cadastros` | agente envia (idempotente); admin lista, aprova (vira família) ou devolve |
| `comum` | `/api/saude` | erros (`ManipuladorDeErros`), `PaginaResposta`, `LimitadorPorIp` |
| `dominio` | — | `Idade` (cálculo com idade estimada datada), `NumerosCalcado`, `Rotulavel` |

Migrações Flyway: V1 a V12 (a próxima é `V13__…`).

**Ainda não existem:** rota de mapa (`/api/relatorios/mapa`, citada pelo web),
exportação para Excel (RF-05), rota para criar agente/código de convite (hoje
só via SQL na tabela `agente`) e rota para o aparelho consultar se o
pré-cadastro foi aprovado ou devolvido.

Ao ajudar aqui, **não construa módulos inteiros por iniciativa própria.** O
time trabalha por issues no Kanban. Faça a tarefa pedida, no tamanho pedido.

## Regras

1. **Português em tudo**: classes, tabelas, colunas, rotas, commits, comentários.
   Colunas `snake_case`, campos Java `camelCase`.
2. **Nada que possa ser calculado é digitado.** Não crie colunas de total
   (total de pessoas, quantos até 12, quantos com 60+). Esses valores são consulta.
3. **Listas fechadas são enums** que implementam `Rotulavel` e aparecem em
   `GET /api/metadados`. Nunca texto livre onde deveria haver lista. Enum novo
   ou valor novo: acrescente em `MetadadosResponse`/`MetadadosService` e avise o
   web, que espelha os tipos em `src/tipos/dominio.ts`.
4. **`ddl-auto: none` sempre.** Schema muda por migração Flyway nova; nunca
   edite uma migração existente.
5. **O cadastro de usuário existe, mas é autenticado** (`POST /api/usuarios`).
   Nunca o torne público. A primeira conta vem do perfil `criar-usuario`.
6. **Trancado por padrão**: `anyRequest().hasRole("ADMIN")`. Para abrir uma rota,
   acrescente-a à lista de `permitAll` em `SegurancaConfig`. O token do
   aparelho da agente (`ROLE_AGENTE`) abre **só** `POST /api/pre-cadastros` e
   `GET /api/comunidades/opcoes` (lista enxuta, sem dados do líder). As duas
   estão listadas explicitamente ali e repetidas com `@PreAuthorize`. Nunca dê
   mais que isso ao token do aparelho, e nunca dado de família (ADR-0002).
7. **Data de nascimento é opcional**; existe `idadeEstimada` + `idadeEstimadaEm`
   (os dois juntos ou nenhum, garantido por `CHECK` no banco). Nunca torne a
   data obrigatória. Pessoa sem nome é aceita com `cadastroIncompleto`.
8. **Mapa é por comunidade**, nunca por família.
9. **Entidade não vira resposta de API.** Use sempre um `record` de DTO.
10. **Família não se apaga, se inativa** (issue #43, V12). Inativa some de
    listagem, contagem e relatório. Não crie `DELETE /api/familias/{id}`.
11. **O `POST /api/pre-cadastros` recebe `JsonNode`**, não o DTO, e guarda o
    JSON bruto em `pre_cadastro.payload`. Assim nenhum campo de uma versão nova
    do app se perde. O `id` vem do aparelho e é a chave de idempotência
    (resposta `JA_RECEBIDO`). Não gere esse id no servidor.
12. Dado real de família não entra em migração de exemplo, teste ou exemplo.
    Log não imprime CPF, senha nem token.

## Testes

`mvn verify` roda JUnit com `@SpringBootTest` + `@ActiveProfiles("test")`:
**H2 em memória, `ddl-auto: create-drop` e Flyway desligado**
(`application-test.yml`). O alias `UNACCENT` do H2 vem de
`src/test/.../suporte/UnaccentH2.java`. Consequências:

- **as migrações não são testadas.** Ao criar uma, suba a API contra o
  Postgres do docker (`docker compose up -d db` + `mvn spring-boot:run`) e
  confira que o Flyway aplica e o Hibernate carrega;
- SQL nativo precisa funcionar em Postgres **e** em H2;
- testes que criam comunidade/família precisam limpar na ordem das FKs
  (família antes de comunidade).

## Antes de mudar arquitetura ou escopo

Leia `docs/decisoes/` e `docs/requisitos.md`. Há duas questões bloqueantes em
aberto vindas da elicitação (Q-01 acesso único vs. mais de 100 colaboradores;
Q-02 cadastro vs. biblioteca de fotos). Se a mudança contraria uma ADR, atualize
a ADR no mesmo PR, não a ignore em silêncio.

As ADRs 0001 e 0002 foram escritas quando a API era NestJS: onde falam em
`pnpm usuario:criar` ou Swagger em `/api/docs`, o atual é o perfil
`criar-usuario` e `/swagger-ui.html` (ADR-0007).
`docs/decisoes/V10__usuario_papel.sql` é só cópia; a migração que vale está em
`src/main/resources/db/migration/`.

## Comandos

```bash
set -a && . ./.env && set +a          # variáveis de .env.example
docker compose up -d db               # Postgres local em localhost:5432
mvn spring-boot:run                   # http://localhost:3333/api
mvn spring-boot:run -Dspring-boot.run.profiles=dev             # + log de SQL
mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario   # cria/redefine a conta
mvn verify                            # o que o CI roda
mvn test -Dtest=PreCadastroTest       # um teste só
```

Swagger em `http://localhost:3333/swagger-ui.html`.

## Skills

Em `.claude/skills/`: `rodar-api-local` (subir, criar conta, dados de teste com
`scripts/semear.sh`, curl), `novo-endpoint-api`, `nova-migracao-flyway`,
`nova-lista-fechada` e `preparar-pr-api`. Mudanças que o web ou o app consomem:
skill `mudanca-de-contrato`, na pasta que agrupa os três repositórios.
