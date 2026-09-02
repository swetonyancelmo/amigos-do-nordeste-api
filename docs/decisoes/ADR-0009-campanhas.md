# ADR-0009 — Campanhas: o que a elicitação mudou de lugar

**Data:** 31/08/2026 · **Situação:** proposta — validar com a associação

## O que apareceu

Perguntada sobre a atividade que mais causa dificuldade, a coordenadora
respondeu: *"Todas têm pontos de dificuldade, mas **as campanhas dão muito
trabalho**"*. As campanhas também aparecem como uma das atividades que mais
ocupam a equipe, junto com o cadastro e a organização das doações recebidas.

## Por que isso importa

Nas decisões anteriores, `campanha` e `entrega` estavam explicitamente **fora do
MVP** — "não construam agora, só deixem o modelo pronto para receber". A
justificativa era que o cadastro vinha primeiro.

A elicitação mostra que o cadastro **existe para servir a campanha**. E que a
campanha é onde está o trabalho. Manter campanha fora do escopo continua sendo
defensável pelo prazo, mas a justificativa mudou: não é mais "não é dor", é
"é dor, e não cabe neste semestre".

## Proposta

Um módulo mínimo de campanha, **se e somente se** as telas do MVP estiverem
prontas até a semana 7:

- `campanha`: nome, período, comunidades atendidas;
- `entrega`: qual família recebeu o quê, em qual campanha;
- e o relatório de necessidades passando a nascer **de uma campanha**, em vez de
  ser uma consulta solta.

O ganho concreto: hoje a associação não sabe dizer quem já recebeu na campanha
anterior. Com isso, sabe — e para de entregar duas vezes para a mesma família e
nenhuma para outra.

## O que não fazer

Não começar por aqui. O cadastro precisa existir antes de a entrega ter a quem
se ligar, e um módulo de campanha sobre um cadastro vazio não se testa.
