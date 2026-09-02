# Cadastro de Famílias — API

Backend do sistema de cadastro das famílias atendidas pela **Associação Amigos
do Nordeste**, no Sertão do Moxotó (PE).

**Java 21 · Spring Boot 3.4 · JPA · Flyway · PostgreSQL**

> ### Este repositório traz só a autenticação, de propósito
>
> A ideia é que o time **não gaste tempo com Spring Security** e vá direto para
> a lógica do sistema. O que está pronto: login, renovação de sessão, troca de
> senha, criação de conta e a configuração de segurança inteira.
>
> **Tudo do domínio — família, pessoa, comunidade, município, fonte de renda,
> relatórios — vocês constroem.** O modelo está desenhado no PDF de
> especificação e nas ADRs, e as tarefas estão no quadro Kanban.

| | |
|---|---|
| Frontend | `cadastro-familias-web` — *(colar a URL aqui)* |
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
mvn spring-boot:run       # o Flyway cria a tabela usuario na subida
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

| Rota | Acesso | O que faz |
|---|---|---|
| `POST /api/auth/login` | pública | Devolve o access token no corpo; o de renovação vai em cookie `httpOnly`. |
| `POST /api/auth/renovar` | pública | Novo access token a partir do cookie. |
| `POST /api/auth/sair` | pública | Limpa o cookie. |
| `POST /api/auth/trocar-senha` | autenticada | Troca a senha da própria conta. |
| `POST /api/usuarios` | **autenticada** | Cria uma conta de acesso. |
| `GET /api/usuarios` | autenticada | Lista as contas. |
| `GET /api/saude` | pública | Monitoramento e ping para acordar o serviço. |

### Sobre o cadastro de usuário

Ele existe, mas **não é público**: só quem já está autenticado cria outra conta.

A dona da associação disse que uma pessoa cadastra e uma pessoa acessa. Num
sistema com uma usuária, um registro aberto seria um buraco de segurança sem
nenhum benefício — qualquer pessoa na internet criaria uma conta e entraria.

A primeira conta, que não tem ninguém autenticado para criá-la, nasce do perfil
`criar-usuario`. E se a questão Q-01 de `docs/requisitos.md` for respondida com
"mais de uma pessoa usa o sistema", este endpoint já é o lugar certo — vai
faltar papel e permissão na tabela `usuario`, não uma rota nova.

---

## Como continuar daqui

Toda rota nova **já nasce protegida**: o `SegurancaConfig` usa
`anyRequest().authenticated()`, e só a lista curta de `permitAll` fica aberta.
Vocês não precisam mexer em segurança para criar um controller — é exatamente o
que esta base resolve.

Para saber quem está autenticado num controller:

```java
@GetMapping
public List<Coisa> listar(@AuthenticationPrincipal String usuarioId) { … }
```

Cada tabela nova entra como uma migração nova em
`src/main/resources/db/migration`: `V2__comunidade.sql`, `V3__familia.sql`…
`ddl-auto` é `none` em todo ambiente, e **migração que já rodou no banco de
alguém nunca se edita** — o Flyway acusa checksum diferente.

Três regras do projeto que valem para tudo que vier:

1. **Nada que possa ser calculado é digitado.** Não crie colunas de total.
2. **Data de nascimento é opcional**, com idade estimada e a data da estimativa.
3. **Todo campo coletado precisa aparecer em algum relatório ou filtro.**

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
