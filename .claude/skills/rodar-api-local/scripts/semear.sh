#!/usr/bin/env bash
# Popula a API com dados FICTÍCIOS para testar à mão: um município, uma
# comunidade e uma agente com código de convite. Nunca use dado real aqui.
# Só fala com a API, então serve tanto para o banco local quanto para o Neon.
#
# Pré-requisitos: API rodando, conta criada pelo perfil criar-usuario, e as
# ferramentas curl e jq.
#
# Uso:
#   EMAIL=... SENHA=... ./semear.sh
# Variável opcional: API (padrão http://localhost:3333).
set -euo pipefail

API="${API:-http://localhost:3333}"
: "${EMAIL:?Defina EMAIL (a conta criada pelo perfil criar-usuario)}"
: "${SENHA:?Defina SENHA}"

for cmd in curl jq; do
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

# Reaproveita a agente de teste se ela já existe: um convite novo derruba o
# aparelho antigo, mas os pré-cadastros continuam ligados a ela.
AGENTE=$(curl -fsS "${AUTH[@]}" "$API/api/agentes" \
  | jq -r '.[] | select(.nome == "Agente de Teste") | .id' | head -1)
if [ -z "$AGENTE" ]; then
  CONVITE=$(curl -fsS -X POST "${AUTH[@]}" "$API/api/agentes" \
    -d '{"nome":"Agente de Teste"}' | jq -r .codigoConvite)
else
  CONVITE=$(curl -fsS -X POST "${AUTH[@]}" "$API/api/agentes/$AGENTE/novo-convite" | jq -r .codigoConvite)
fi

cat <<FIM
Pronto.
  município:  $MUNICIPIO
  comunidade: $COMUNIDADE
  convite da agente: $CONVITE  (vale uma vez; troque em POST /api/agentes/ativar)
FIM
