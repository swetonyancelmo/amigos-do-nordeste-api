# Cadastro de Famílias — API

Backend do sistema de cadastro das famílias atendidas pela **Associação Amigos
do Nordeste**, no Sertão do Moxotó (PE).

**Java 21 · Spring Boot 3.4 · JPA · Flyway · PostgreSQL**

A API atende dois clientes: o **painel web** da associação e o **app de campo**
da agente de saúde, que envia pré-cadastros para aprovação. O projeto começou só
com a autenticação, para o time não gastar tempo com Spring Security. O domínio
foi construído depois, a partir das tarefas do quadro Kanban.

| | |
|---|---|
| Frontend web | [`cadastro-familias-web`](https://github.com/swetonyancelmo/amigos-do-nordeste-web) |
| App de campo | [`cadastro-familias-app`](https://github.com/swetonyancelmo/amigos-do-nordeste-app) |
| Protótipo (Figma) | https://www.figma.com/design/dEZbIRWGGdOQEsvAtsmFxQ |
| Especificação | [`docs/Especificacao-Sistema-Cadastro-Familias.pdf`](docs/) |
| Requisitos e questões em aberto | [`docs/requisitos.md`](docs/requisitos.md) |
| Decisões de arquitetura | [`docs/decisoes/`](docs/decisoes/) |

---

## Rodando

Precisa de **JDK 21** e **Docker** (só para o banco).

```bash
cp .env.example .env      # e troque o APP_JWT_SEGREDO
set -a && . ./.env && set +a

docker compose up -d db
mvn spring-boot:run       # o Flyway aplica as migrações na subida
```

API em **http://localhost:3333/api** · Swagger em
**http://localhost:3333/swagger-ui.html**

Primeira conta (uma vez, na instalação):

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario
```

A senha aparece **uma vez** no terminal. Gere o segredo do JWT com
`openssl rand -base64 48`.

> O projeto não inclui o Maven Wrapper. Se quiserem fixar a versão do Maven para
> todo mundo, rodem `mvn wrapper:wrapper` uma vez e commitem o `mvnw`, o
> `mvnw.cmd` e a pasta `.mvn/`.

---

## O que já existe

Toda rota é **ADMIN** (JWT da usuária do painel), exceto as marcadas. O detalhe
de cada corpo e resposta está no Swagger.

### Acesso e contas

| Rota | Acesso | O que faz |
|---|---|---|
| `POST /api/auth/login` | pública | Devolve o access token no corpo; o de renovação vai em cookie `httpOnly`. |
| `POST /api/auth/renovar` | pública | Novo access token a partir do cookie. |
| `POST /api/auth/sair` | pública | Limpa o cookie. |
| `POST /api/auth/trocar-senha` | admin | Troca a senha da própria conta (exige a senha atual). |
| `POST /api/usuarios` | admin | Cria uma conta de acesso. |
| `GET /api/usuarios` | admin | Lista as contas. |
| `POST /api/agentes` | admin | Cadastra a agente e devolve o código de convite de 6 dígitos. |
| `GET /api/agentes` | admin | Lista as agentes, com o convite ainda pendente. |
| `POST /api/agentes/{id}/novo-convite` | admin | Código novo para a mesma agente; o aparelho antigo perde o acesso. |
| `POST /api/agentes/ativar` | pública, 5/min por IP | Troca o código de convite de 6 dígitos pelo token do aparelho. |
| `GET /api/saude` | pública | Monitoramento e ping para acordar o serviço. |
| `GET /api/metadados` | pública | Todas as listas fechadas (enums) com rótulo em português. |

### Cadastro

| Rota | O que faz |
|---|---|
| `GET/POST /api/municipios`, `GET/PUT /api/municipios/{id}` | Municípios atendidos (`codigo_ibge` único). |
| `GET/POST /api/comunidades`, `GET/PUT /api/comunidades/{id}` | Comunidades, com filtro por município e latitude/longitude para o mapa. |
| `GET /api/comunidades/opcoes` | **Agente.** Só id, nome e município, para o app escolher offline. |
| `GET /api/familias` | Lista paginada. Filtros: `busca`, `municipioId`, `comunidadeId`, `semBanheiro`, `incluirInativas`, `pagina`, `porPagina` (25, máx. 100). |
| `GET /api/familias/{id}` | Ficha completa: comunidade, município, membros, fontes de renda e totais calculados. |
| `POST /api/familias`, `PUT /api/familias/{id}` | Família com pessoas e fontes de renda numa chamada só (o `PUT` faz merge). |
| `POST /api/familias/{id}/inativar` · `/reativar` | Família não se apaga: inativa some de listagem, contagem e relatório. |
| `GET /api/pessoas`, `GET /api/pessoas/{id}` | Lista com filtros (`nome`, `comunidadeId`, `municipioId`, `familiaId`, `cadastroIncompleto`, `estuda`, `faixaEtaria`, `pagina`, `tamanho`) e ficha. |
| `POST /api/familias/{familiaId}/pessoas`, `PUT/DELETE /api/pessoas/{id}` | Inclui, edita e remove pessoa de uma família. |

### Pré-cadastro (app de campo)

| Rota | Acesso | O que faz |
|---|---|---|
| `POST /api/pre-cadastros` | **agente** | Recebe um pré-cadastro. Idempotente pelo `id` gerado no aparelho: reenvio responde `JA_RECEBIDO`; um `DEVOLVIDO` reenviado pela mesma agente volta a `PENDENTE` com o payload corrigido. |
| `GET /api/pre-cadastros/situacao?ids=` | **agente** | Situação do que o próprio aparelho enviou (id, situação e motivo da devolução), até 100 ids. |
| `GET /api/pre-cadastros?situacao=` | admin | Fila de chamados, com aviso de possível duplicata (mesma comunidade e telefone ou nome parecido). |
| `GET /api/pre-cadastros/{id}` | admin | Ficha do chamado: o que a agente coletou, com o índice de cada pessoa usado na aprovação. |
| `POST /api/pre-cadastros/{id}/aprovar` | admin | Vira família, com o complemento opcional de moradia, renda e dados por pessoa. |
| `POST /api/pre-cadastros/{id}/devolver` | admin | Devolve com motivo obrigatório. |

### Relatórios

| Rota | O que faz |
|---|---|
| `GET /api/relatorios/necessidades` | Roupa e calçado por tamanho (`comunidadeId`, `municipioId`, `todasIdades`; padrão: só até 12 anos). |
| `GET /api/relatorios/situacao` | Indicadores de situação das famílias, em valor e percentual (`comunidadeId`, `municipioId`). |

### Ainda não existe

- `GET /api/relatorios/mapa` (pontos por comunidade, ADR-0005);
- exportação para Excel (RF-05);
- rota para criar agente e gerar código de convite: hoje o registro é inserido
  direto na tabela `agente`;
- rota para o aparelho saber se o pré-cadastro foi aprovado ou devolvido.

### Sobre o cadastro de usuário

Ele existe, mas **não é público**: só quem já está autenticado cria outra conta.

A dona da associação disse que uma pessoa cadastra e uma pessoa acessa. Num
sistema com uma usuária, um registro aberto seria um buraco de segurança sem
nenhum benefício — qualquer pessoa na internet criaria uma conta e entraria.

A primeira conta, que não tem ninguém autenticado para criá-la, nasce do perfil
`criar-usuario`. E se a questão Q-01 de `docs/requisitos.md` for respondida com
"mais de uma pessoa usa o sistema", este endpoint já é o lugar certo: a coluna
`papel` já existe (V10), falta só criar papéis além de `ADMIN`.

---

## Como continuar daqui

Toda rota nova **já nasce protegida**: o `SegurancaConfig` usa
`anyRequest().hasRole("ADMIN")`, e só a lista curta de `permitAll` fica aberta.
Vocês não precisam mexer em segurança para criar um controller. Rota para o
aparelho da agente (`ROLE_AGENTE`) só entra listada ali explicitamente e
repetida com `@PreAuthorize` no controller.

Para saber quem está autenticado num controller:

```java
@GetMapping
public List<Coisa> listar(@AuthenticationPrincipal String usuarioId) { … }
```

Cada tabela ou coluna nova entra como uma migração nova em
`src/main/resources/db/migration`. Já existem V1 a V12, então a próxima é
`V13__descricao.sql`. `ddl-auto` é `none` em todo ambiente, e **migração que já
rodou no banco de alguém nunca se edita**: o Flyway acusa checksum diferente.

Enum novo que aparece em tela implementa `Rotulavel` e entra em
`GET /api/metadados`. O web monta os `<select>` a partir dali.

### Testes

```bash
mvn verify                        # o CI roda o mesmo
mvn test -Dtest=PreCadastroTest   # um teste só
```

Os testes de integração rodam com o perfil `test`: **H2 em memória, schema
gerado pelo Hibernate e Flyway desligado**. Por isso `mvn verify` não pega erro
de migração. Depois de criar uma, suba a API contra o Postgres do docker e
confira se ela aplica.

Regras do projeto que valem para tudo que vier:

1. **Nada que possa ser calculado é digitado.** Não crie colunas de total.
2. **Data de nascimento é opcional**, com idade estimada e a data da estimativa.
3. **Todo campo coletado precisa aparecer em algum relatório ou filtro.**
4. **Família não se apaga, se inativa.**
5. **Entidade não vira resposta**: sempre um `record` de DTO.

---

## Segurança, em uma tabela

| Decisão | Por quê |
|---|---|
| Hash **argon2id** | Senha nunca em texto; hash rápido é o que não se quer aqui. |
| Access token de 15 min, em memória no front | Token em `localStorage` é o que um XSS leva embora. |
| Renovação em cookie `httpOnly` + `SameSite=Lax` | O JavaScript da página não consegue ler. |
| Tudo autenticado por padrão | Rota nova nasce fechada, não aberta. |
| Mesma mensagem para e-mail e senha errados | Dizer qual dos dois falhou entrega metade da resposta. |
| Sem rota pública de registro | Ver acima. |

Detalhes e alternativas consideradas na
[ADR-0002](docs/decisoes/ADR-0002-autenticacao.md).

---

Dado real de família **não entra em repositório** — nem em migração de exemplo,
nem em teste, nem em issue.
