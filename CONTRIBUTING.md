# Como contribuir — API

## Antes do PR

```bash
mvn verify
```

O CI roda o mesmo, com um Postgres de serviço.

## Fluxo

1. Branch a partir da `main`: `feat/`, `fix/`, `docs/`, `chore/` + descrição em
   kebab-case.
2. Commits em Conventional Commits, em português:
   `feat(familia): calcula totais por faixa etária`
3. **Mudou algo que o frontend consome? Avise a outra equipe no mesmo dia.** Com
   repositórios separados, não existe compilador para avisar por você — o erro
   só aparece quando alguém abre a tela.
4. Merge com CI verde e uma aprovação.

## Banco

- Tabela nova ou entidade alterada? Crie uma migração nova:
  `V2__comunidade.sql`, `V3__familia.sql`, e assim por diante.
- **Nunca edite uma migração que já rodou** no banco de outra pessoa. O Flyway
  guarda o checksum e vai reclamar — e com razão.
- `ddl-auto` fica `none`. Se você está pensando em ligar `update` para "resolver
  rápido", é aí que os bancos começam a divergir.

## Nomes

Português em tudo: classes, tabelas, colunas, rotas, commits, comentários.
Colunas em `snake_case`, campos Java em `camelCase`.

Entidade não vira resposta de API — use `record` de DTO. Se a entidade vazar
para a resposta, uma mudança interna no modelo quebra a tela sem ninguém
perceber, e num projeto com dois repositórios isso demora a aparecer.

## Dados sensíveis

O cadastro tem dados de menores, renda e condição de moradia. A própria
associação declarou preocupação com golpes envolvendo seus dados.

- Nada de dado real em migração de exemplo, teste, issue, print ou no chat do grupo.
- `.env` nunca é commitado. Se um segredo vazar, **troque o `APP_JWT_SEGREDO`** —
  não basta apagar o commit.
- Log não imprime CPF, senha nem token.
