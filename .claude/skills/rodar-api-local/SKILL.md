---
name: rodar-api-local
description: Sobe a API do cadastro de famílias localmente (Postgres no docker compose + Spring Boot na porta 3333), cria a conta de acesso, popula dados fictícios de teste (município, comunidade, agente com código de convite) e mostra como chamar as rotas com curl usando o token de admin ou o token do aparelho da agente. Use quando o usuário quiser rodar, testar à mão, "bater na API", pegar um token, ver o Swagger, reproduzir um bug de rota, testar o fluxo de pré-cadastro, ou quando a API não sobe (erro de Flyway, banco, porta, JWT).
---

# Rodar a API localmente

Tudo aqui é para desenvolvimento. Nenhum dado real de família entra no banco
local, nem em print de tela.

## 1. Banco e variáveis

```bash
cp -n .env.example .env              # só na primeira vez
set -a && . ./.env && set +a         # exporta as variáveis para este shell
docker compose up -d db              # Postgres 16, container cadastro-familias-db
```

Troque `APP_JWT_SEGREDO` no `.env` por um valor de verdade
(`openssl rand -base64 48`). O segredo padrão funciona, mas não deve sair da
máquina de ninguém.

Os dados do banco ficam em `./postgres-data/` (ignorado pelo git). Para
recomeçar do zero: `docker compose down && rm -rf postgres-data` (confirme com
o usuário antes, porque isso apaga o banco local inteiro).

## 2. Subir

```bash
mvn spring-boot:run                                  # http://localhost:3333/api
mvn spring-boot:run -Dspring-boot.run.profiles=dev   # mesmo, com log de SQL
```

Rode em segundo plano quando precisar continuar usando o terminal. A API está
pronta quando `curl -s localhost:3333/api/saude` responder.
Swagger: `http://localhost:3333/swagger-ui.html`.

## 3. Conta de acesso (uma vez)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario
# ou com senha escolhida:
mvn spring-boot:run -Dspring-boot.run.profiles=criar-usuario -Dspring-boot.run.arguments=--senha="senha-de-teste-123"
```

O e-mail vem de `APP_USUARIO_INICIAL_EMAIL`. A senha aparece uma vez no
terminal. Rodar de novo redefine a senha, e é assim que se recupera acesso.
Esse perfil sobe a aplicação inteira: depois que a senha aparecer, encerre o
processo.

## 4. Dados de teste

```bash
EMAIL=... SENHA=... .claude/skills/rodar-api-local/scripts/semear.sh
```

O script cria, sem duplicar, o "Município de Teste", o "Sítio de Teste" e a
"Agente de Teste", e imprime um código de convite novo (seis dígitos, gerado
pela API em `POST /api/agentes` ou `POST /api/agentes/{id}/novo-convite`).
Precisa de `curl` e `jq`; como só fala com a API, também serve para o Neon
(`API=https://... ./semear.sh`).

## 5. Chamando as rotas

```bash
API=http://localhost:3333
TOKEN=$(curl -s -X POST $API/api/auth/login -H 'Content-Type: application/json' \
  -d '{"email":"...","senha":"..."}' | jq -r .accessToken)
curl -s $API/api/familias -H "Authorization: Bearer $TOKEN" | jq

# token do aparelho da agente (o convite só vale uma vez)
AGENTE=$(curl -s -X POST $API/api/agentes/ativar -H 'Content-Type: application/json' \
  -d '{"codigo":"123456"}' | jq -r .token)
curl -s $API/api/comunidades/opcoes -H "Authorization: Bearer $AGENTE" | jq
```

O access token dura 15 minutos: quando der `401`, faça login de novo. O login
aceita 5 tentativas por minuto por IP, e a ativação de agente também.

Para testar o pré-cadastro, envie com o token da agente um JSON com `id` (UUID
novo), `responsavelNome`, `comunidadeId`, `criadoEm` e `pessoas`. O formato
exato está no Swagger (`EnviarPreCadastroRequisicao`). Reenviar o mesmo `id`
deve responder `JA_RECEBIDO`.

## Quando não sobe

| Sintoma | Causa provável |
|---|---|
| `Connection refused` na 5432 | `docker compose up -d db` não rodou, ou outra instância do Postgres ocupa a porta |
| `FlywayValidateException ... checksum mismatch` | alguém editou uma migração já aplicada. Não edite de volta sem entender: veja a skill `nova-migracao-flyway` |
| `column ... does not exist` na primeira consulta | entidade mudou sem migração nova (`ddl-auto: none` não valida na subida, então o erro só aparece ao usar a rota) |
| erro de chave JWT curta | `APP_JWT_SEGREDO` com menos de 32 bytes |
| web recebe erro de CORS | `APP_CORS_ORIGENS` não inclui `http://localhost:3000` |
| variáveis ignoradas | o `.env` não foi exportado neste shell (`set -a && . ./.env && set +a`) |
