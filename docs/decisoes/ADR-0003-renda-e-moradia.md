# ADR-0003 — Renda e moradia no modelo

**Data:** 27/08/2026 · **Situação:** aceita

## Contexto

Depois da reunião, a associação perguntou se dava para acrescentar "renda (bolsa
família, aposentadoria, BPC, trabalho sazonal), cisterna, poço, banheiro".

São duas coisas de naturezas diferentes.

## Decisões

**1. Moradia são campos de `familia`, não uma entidade `domicilio`.**
Em sítio e assentamento a relação é 1 família : 1 casa na prática. A entidade
separada só compraria um join e uma tela a mais. Se um dia aparecerem duas
famílias no mesmo domicílio, separa-se ali.

**2. Água é múltipla escolha, não dois booleanos.**
`tem_cisterna` + `tem_poco` travaria no primeiro caso de carro-pipa — que é o
mais revelador dos três no sertão. É um `text[]`.

**3. As categorias são as da Ficha de Cadastro Domiciliar do e-SUS.**
Não inventamos lista. Usando o mesmo vocabulário, os números da associação ficam
comparáveis com dado oficial, o que vale em edital e em conversa com prefeitura.

**4. Renda é uma tabela filha com dono opcional.**
`fonte_renda` pende de `familia` e tem `pessoa_id` **nullable**. Bolsa Família é
benefício da família e costuma chegar sem dono no papel do líder; BPC,
aposentadoria e trabalho são de uma pessoa. Campo obrigatório aqui travaria a
digitação.

**5. Faixa em salários mínimos, nunca valor em reais.**
Renda declarada por um vizinho, no papel, sobre trabalho sazonal, é o dado menos
confiável do cadastro — e o mais sensível. Para priorizar doação, *quais fontes*
mais *quantos dependentes* já ordena as famílias.

## Consequência que não pode ser esquecida

Todo campo coletado precisa aparecer em pelo menos um relatório ou filtro
(`/relatorios/situacao` cobre estes). Campo que ninguém lê é digitação sem
retorno — e, em LGPD, dado coletado sem finalidade.
