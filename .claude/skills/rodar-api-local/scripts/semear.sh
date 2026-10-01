#!/usr/bin/env bash
# Popula a API local com dados FICTÍCIOS para testar à mão: um município, uma
# comunidade e uma agente com código de convite. Nunca use dado real aqui.
#
# Pré-requisitos: API rodando, Postgres do docker compose de pé, conta criada
# pelo perfil criar-usuario, e as ferramentas curl, jq e docker.
#
# Uso:
#   EMAIL=... SENHA=... ./semear.sh
# Variáveis opcionais: API (padrão http://localhost:3333), CONVITE (padrão 123456),
# CONTAINER (padrão cadastro-familias-db).
set -euo pipefail

API="${API:-http://localhost:3333}"
CONVITE="${CONVITE:-123456}"
CONTAINER="${CONTAINER:-cadastro-familias-db}"
: "${EMAIL:?Defina EMAIL (a conta criada pelo perfil criar-usuario)}"
: "${SENHA:?Defina SENHA}"

for cmd in curl jq docker; do
  command -v "$cmd" >/dev/null || { echo "Falta o comando '$cmd'." >&2; exit 1; }
done

curl -fsS "$API/api/saude" >/dev/null || { echo "A API não responde em $API." >&2; exit 1; }

TOKEN=$(curl -fsS -X POST "$API/api/auth/login" -H 'Content-Type: application/json' \
  -d "$(jq -n --arg e "$EMAIL" --arg s "$SENHA" '{email:$e, senha:$s}')" | jq -r .accessToken)
AUTH=(-H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json')

MUNICIPIO=$(curl -fsS "${AUTH[@]}" "$API/api/municipios" \
  | jq -r '.[] | select(.nome == "Município de Teste") | .id' | head -1)
if [ -z "$MUNICIPIO" ]; then
  MUNICIPIO=$(curl -fsS -X POST "${AUTH[@]}" "$API/api/municipios" \
    -d '{"nome":"Município de Teste","uf":"PE","codigoIbge":""}' | jq -r .id)
fi

COMUNIDADE=$(curl -fsS "${AUTH[@]}" "$API/api/comunidades" \
  | jq -r '.[] | select(.nome == "Sítio de Teste") | .id' | head -1)
if [ -z "$COMUNIDADE" ]; then
  COMUNIDADE=$(curl -fsS -X POST "${AUTH[@]}" "$API/api/comunidades" \
    -d "$(jq -n --arg m "$MUNICIPIO" '{nome:"Sítio de Teste", municipioId:$m, tipoComunidade:"SITIO"}')" \
    | jq -r .id)
fi

# Não existe rota para criar agente: o registro entra direto no banco.
docker exec -i "$CONTAINER" psql -q -U and -d cadastro <<SQL
INSERT INTO agente (nome, codigo_convite)
SELECT 'Agente de Teste', '$CONVITE'
WHERE NOT EXISTS (SELECT 1 FROM agente WHERE codigo_convite = '$CONVITE');
SQL

cat <<FIM
Pronto.
  município:  $MUNICIPIO
  comunidade: $COMUNIDADE
  convite da agente: $CONVITE  (vale uma vez; troque em POST /api/agentes/ativar)
FIM
