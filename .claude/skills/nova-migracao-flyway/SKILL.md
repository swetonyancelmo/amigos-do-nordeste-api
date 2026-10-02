---
name: nova-migracao-flyway
description: Cria e valida uma migração Flyway nova na API do cadastro de famílias (src/main/resources/db/migration, V13 em diante), mantendo entidade JPA, CHECKs e dados existentes coerentes. Use sempre que uma tarefa acrescentar ou alterar tabela, coluna, índice, constraint ou valores gravados, quando mudar uma entidade @Entity, quando o usuário falar em "migration", "migração", "alterar o banco", "nova coluna", "Flyway", ou quando aparecer erro de checksum ou coluna inexistente.
---

# Nova migração Flyway

O schema só muda por migração nova. `ddl-auto` é `none` em todos os ambientes,
e **migração que já rodou no banco de alguém nunca se edita**: o Flyway guarda
o checksum e passa a recusar a subida em toda máquina que já a aplicou,
inclusive produção (Neon).

## Passo a passo

1. **Descubra o próximo número:**
   ```bash
   ls src/main/resources/db/migration | sort -V | tail -3
   ```
   Use o próximo inteiro livre (`V13__`, `V14__`…). Não use sub-versões
   (`V12_1__`). Verifique também se não há um PR aberto usando o mesmo número;
   se houver, combine com a outra pessoa antes do merge.
2. **Nomeie em português e em snake_case**, dizendo o que muda:
   `V13__familia_ocupacao.sql`, `V14__pre_cadastro_consulta_agente.sql`.
3. **Escreva o SQL para Postgres 16**, com um comentário no topo explicando o
   porquê e citando a issue, no estilo das migrações existentes:
   ```sql
   -- Issue #57: a ocupação passa a ser coletada para o programa de geração de
   -- renda. Opcional: famílias já cadastradas ficam com NULL.
   ALTER TABLE pessoa ADD COLUMN ocupacao varchar(40);
   ```
4. **Pense nos dados que já existem.** Coluna `NOT NULL` nova precisa de
   `DEFAULT` (veja V10 e V12). Enum que perde valor precisa converter ou anular
   o que estava gravado (veja V11). Nunca apague dado de família em migração;
   família se inativa (`ativa = false`).
5. **Ajuste a entidade JPA** no mesmo commit: `@Column(name = "...")` com
   tamanho igual ao do SQL, enums com `@Enumerated(EnumType.STRING)`. Tipos de
   data e hora com fuso são `timestamptz` ↔ `OffsetDateTime`.
6. **Respeite as regras de domínio no schema:**
   - nada de coluna de total ou contagem (é consulta);
   - data de nascimento continua opcional; `idade_estimada` e
     `idade_estimada_em` andam juntas (`chk_pessoa_idade`);
   - lista fechada é `varchar` com o `name()` do enum, nunca tabela de texto
     livre;
   - FK nova para `familia` pensa no `ON DELETE`: família não é apagada, mas
     pessoa e fonte de renda são.
7. **Valide contra o Postgres de verdade.** Os testes (`mvn verify`) usam H2
   com o Flyway **desligado** e o schema gerado pelas entidades, então eles não
   executam a sua migração. Rode:
   ```bash
   docker compose up -d db
   set -a && . ./.env && set +a
   mvn spring-boot:run      # deve logar "Successfully applied 1 migration"
   ```
   Depois chame uma rota que use a tabela alterada (skill `rodar-api-local`).
   Ainda assim, rode `mvn verify`, porque a entidade alterada muda o schema do H2.
8. **SQL nativo** em repositório (`@Query(nativeQuery = true)`) precisa
   funcionar em Postgres **e** em H2. `unaccent` já tem alias no H2 de teste.

## Se a migração já foi aplicada e está errada

Não edite. Crie outra migração que corrija. Só é aceitável editar uma migração
que **nunca saiu da sua máquina** (não foi commitada nem aplicada em outro
banco). Nesse caso, para reaplicar localmente, apague a linha dela em
`flyway_schema_history` ou recrie o banco local, e confirme com o usuário antes
de apagar qualquer coisa.

## Checklist antes do commit

- [ ] número novo e único, nome descritivo em português
- [ ] comentário no topo com o porquê
- [ ] entidade ajustada; DTO de resposta ajustado se o campo aparece na API
- [ ] subiu contra o Postgres local sem erro
- [ ] `mvn verify` passou
- [ ] se o campo aparece para o web ou o app, siga a skill `mudanca-de-contrato` (na pasta que agrupa os três repositórios)
- [ ] todo campo coletado aparece em algum relatório ou filtro (ADR-0003)
