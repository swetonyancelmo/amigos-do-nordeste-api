# ADR-0005 — Mapa por comunidade, e o papel do código IBGE

**Data:** 27/08/2026 · **Situação:** aceita

## Decisão

O mapa mostra **um ponto por comunidade**, nunca por família.

## Por quê

Um pino por família exigiria a coordenada de cada casa, e **não existe momento
no processo em que ela poderia ser capturada**: o líder escreve no papel, e a
digitação acontece na sede, longe das casas. Endereço rural também não
geocodifica — "perto da igreja" não vira coordenada.

Um pino por comunidade são poucas dezenas de pontos que nunca mudam. Alguém
marca cada um uma vez, no mini-mapa do formulário da comunidade, e acabou.

## Identificador não é coordenada

`municipio.codigo_ibge` **seleciona linhas** — e permite buscar a malha
(contorno) do município na API pública do IBGE e enquadrar o mapa sozinho. Ele
**não posiciona pontos**. Num sistema de saúde os pinos aparecem porque a tabela
de estabelecimentos já traz lat/long vinda do CNES; não existe equivalente para
sítios.

Fontes públicas que podem cobrir parte das comunidades, se sobrar tempo: o
acervo fundiário do **INCRA** (assentamentos têm polígono) e o **CNEFE** do
Censo 2022. Ambos são baixar arquivo e casar nome por aproximação — para quinze
comunidades, clicar quinze vezes no mapa é mais rápido.

## Município é filtro, não tenant

Num sistema multi-tenant de saúde, o município é o cliente e decide até em qual
banco a API conecta. Aqui é o contrário: **uma pessoa atendendo vários
municípios ao mesmo tempo**. Município é atributo da comunidade, e o seletor é
um filtro com "todos" como padrão. Copiar o padrão de tenant faria a usuária
trocar de contexto para ver os próprios dados.

## Privacidade

Um mapa de *famílias* carregando renda e saneamento é, na prática, um mapa de
quem é pobre e onde mora. No nível de comunidade essa preocupação desaparece: um
pino sobre um sítio com "24 famílias" não expõe ninguém.
