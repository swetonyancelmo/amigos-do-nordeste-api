---
name: preparar-pr-api
description: "Prepara um commit ou pull request no repositório da API do cadastro de famílias: roda as verificações (mvn verify), revisa o diff contra as regras do projeto (migração, segurança, DTO, dados sensíveis), escreve a mensagem em Conventional Commits em português e preenche o template de PR. Use quando o usuário pedir para commitar, abrir PR, 'subir', 'mandar pra revisão', revisar antes do merge, ou quando uma tarefa na API terminar."
---

# Preparar commit e PR — API

## 1. Verificar

```bash
mvn verify
```

Se falhar, mostre a saída e corrija antes de continuar; não abra PR com CI
vermelho. Se o diff tem migração nova, confirme também que ela sobe contra o
Postgres local (skill `nova-migracao-flyway`), porque o `mvn verify` roda em
H2 sem o Flyway.

## 2. Revisar o diff

```bash
git status --short && git diff main...HEAD --stat && git diff
```

Procure especificamente:

- **migração existente editada** (`git diff --name-status main...HEAD -- src/main/resources/db/migration` com `M` em vez de `A`): isso quebra o banco de todo mundo;
- **entidade na resposta** de controller, em vez de `record`;
- **`SegurancaConfig` alterado**, ou `permitAll`/`hasRole('AGENTE')` novo: precisa de justificativa explícita e de acordo com a ADR-0002;
- **dado real** (nome, CPF, telefone de pessoa de verdade) em teste, migração ou exemplo do Swagger;
- `.env`, segredo ou token no diff; `System.out`/log com CPF, senha ou token (a única exceção é o `CriarUsuarioRunner`);
- coluna de total ou valor calculado sendo gravado;
- mudança em enum, DTO ou rota que o web ou o app consome.

Aponte o que encontrar ao usuário antes de commitar.

## 3. Commit

Branch a partir da `main`: `feat/`, `fix/`, `docs/`, `chore/`, `test/`,
`refactor/` + descrição em kebab-case (`feat/43-inativar-familia`). Só commite
direto na `main` se o usuário pedir.

Mensagem em **Conventional Commits, em português**, escopo = módulo:

```
feat(familia): inativar e reativar família em vez de excluir (issue #43)

Apagar levaria pessoas, fontes de renda e o histórico de contagem junto.
```

Uma mudança por commit quando der. Cite a issue quando houver.

## 4. PR

Use o template `.github/pull_request_template.md`:

- **O que muda**: uma ou duas frases.
- **Por quê**: o ID do requisito de `docs/requisitos.md` (RF-xx) ou a issue.
- **Como testar**: passos com `curl` ou Swagger, usando dados fictícios.
- **Checklist**: marque só o que é verdade. Em "Mudei algo que o frontend
  consome?", diga exatamente o quê (rota, campo, valor de enum) para a outra
  equipe.

Crie com `gh pr create` só se o usuário pedir para abrir o PR; caso contrário,
entregue o texto pronto. Merge exige CI verde e uma aprovação.
