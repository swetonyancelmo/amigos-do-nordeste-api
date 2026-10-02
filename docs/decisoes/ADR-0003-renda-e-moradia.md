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

## Revisão de 01/10/2026: a faixa é da família, não de cada fonte

A decisão 4 deixou a faixa em cada `fonte_renda`. Na prática isso não respondia
à pergunta que importa — *quanto entra na casa?* —, porque faixa não se soma:
"até 1 salário" + "até 1 salário" pode ser qualquer coisa entre 0 e 2. Na tela
também confundia: parecia que a renda era cadastrada duas vezes, uma por
pessoa e outra pela família.

**Agora (V14):**

- `familia.faixa_renda` guarda **uma** faixa: a renda da casa somando tudo;
- `fonte_renda` fica só com **tipo** e **dono opcional** (decisão 4 continua
  valendo nessa parte): diz de onde o dinheiro vem, não quanto é;
- o tipo `NENHUMA` saiu da lista: família sem renda é a faixa `SEM_RENDA_FIXA`.

Somar renda por pessoa foi descartado pelo mesmo motivo da decisão 5: exigiria
valor em reais por pessoa, o dado menos confiável e mais sensível do cadastro.

Na migração, a faixa antiga só foi aproveitada onde já era a renda da casa (família
com uma fonte só); com duas ou mais fontes ela fica em branco para quem revisar.

## Consequência que não pode ser esquecida

Todo campo coletado precisa aparecer em pelo menos um relatório ou filtro
(`/relatorios/situacao` cobre estes). Campo que ninguém lê é digitação sem
retorno — e, em LGPD, dado coletado sem finalidade.
