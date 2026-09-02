# ADR-0008 — Fotos e vídeos: o que fazer com o pedido mais forte do formulário

**Data:** 31/08/2026 · **Situação:** proposta — **precisa da decisão do grupo e da associação**

## O que apareceu

No formulário de elicitação, a coordenadora de projeto respondeu **duas vezes**,
em perguntas diferentes, que o maior problema não é o cadastro:

> **Maior problema de tecnologia ou organização:** "Um programa capaz de armazenar as fotos de forma independente."
>
> **Outro problema ou sugestão:** "Um programa capaz de unir os registros de fotos e vídeos. Atualmente está tudo disperso."

E confirmou que já perderam arquivo importante: **"fotos dos nossos registros"**.
O equipamento que ela gostaria de ter é um **drone** — coerente com o mesmo tema.

Fotos também aparecem como algo que já acompanham por projeto ("Fotos/documentos").

## Por que isto é sério

É a única resposta que ela repetiu espontaneamente, e há uma perda concreta já
ocorrida. Ignorar seria escolher o problema que nós já tínhamos começado a
resolver em vez do problema que a pessoa entrevistada nomeou.

## Por que ainda assim não vira o projeto

Três razões, nesta ordem:

1. **A decisão é da dona da associação**, e ela pediu o cadastro. Karla é
   coordenadora de projeto — vê outra parte da operação. Duas pessoas, duas
   dores reais, prioridades diferentes. Quem decide precisa ser a associação,
   não o grupo.
2. **Vídeo é um problema de custo, não de código.** Uma biblioteca de mídia é
   armazenamento, transcodificação e banda. Os planos gratuitos que sustentam o
   resto do projeto não sustentam isso, e a associação não tem quem mantenha
   infraestrutura (o suporte técnico é externo).
3. **Google Drive já faz 80% disso** e eles já usam. O problema declarado é
   "está tudo disperso" — que é de **organização**, não de ferramenta. Uma
   convenção de pastas mais uma rotina de backup resolveria boa parte sem uma
   linha de código, e essa é a resposta honesta.

## Proposta

**Não construir uma biblioteca de mídia.** Construir a ponte que faz sentido no
sistema que já existe:

- **anexar fotos a uma família, a uma comunidade ou a uma campanha** — a foto
  passa a ter dono e contexto, que é justamente o que "disperso" significa;
- só imagem, com limite de tamanho, sem vídeo;
- armazenamento em serviço de objetos com camada gratuita, e o banco guardando
  apenas a referência.

Isso resolve o caso que interessa à associação de verdade — **prestação de
contas ao doador**: foto da entrega ligada à comunidade que recebeu. É também o
que dá força ao relatório que já existe.

## O que precisa ser perguntado antes de construir

1. As fotos que se perderam eram de **quê**? De entrega, de família, de evento?
   A resposta diz onde a foto deve ser pendurada.
2. Quem tira as fotos, e de qual aparelho elas saem hoje?
3. Quantas fotos por campanha, aproximadamente? Dezenas ou milhares muda tudo.
4. **Vídeo é essencial ou é "seria bom"?** Se for essencial, a resposta honesta
   é que isso não cabe num projeto de semestre com hospedagem gratuita, e o
   caminho é organizar o Google Drive.

## Se a associação disser que a prioridade é a mídia

Então o grupo deve **mudar o projeto**, não empilhar mais uma funcionalidade.
Nesse caso vale refazer a elicitação com a dona presente, porque o sistema seria
outro — e é melhor descobrir isso agora do que na semana 9.
