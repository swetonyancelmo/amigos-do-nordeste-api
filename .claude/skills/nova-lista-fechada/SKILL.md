---
name: nova-lista-fechada
description: Cria ou altera uma lista fechada (enum Java que implementa Rotulavel) na API do cadastro de famílias e a publica em GET /api/metadados, com migração para os valores já gravados e aviso ao web e ao app. Use quando a tarefa envolver opções de seleção, categorias, "lista de valores", tamanhos, séries, tipos, parentesco, situação, quando alguém quiser acrescentar, renomear ou remover uma opção ("a associação pediu uma opção nova de renda"), ou quando um campo de texto livre deveria virar lista.
---

# Lista fechada (enum + /api/metadados)

Campo categórico nunca é texto livre: "Sitio Igrejinha", "sítio igrejinha" e
"Igrejinha" viram três valores e o relatório deixa de fechar. Toda lista
fechada é um enum Java, e o web monta os `<select>` a partir de
`GET /api/metadados` em vez de manter uma cópia. Assim, quando a associação
pedir uma opção nova, ela aparece sem mexer no frontend.

## Criar um enum novo

1. Crie em `<modulo>/enums/`, implementando `dominio.Rotulavel`, com o rótulo
   em português como a usuária vai ler:
   ```java
   package br.org.amigosdonordeste.cadastro.pessoa.enums;

   import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

   /** Lista fechada, servida em GET /api/metadados. */
   public enum Ocupacao implements Rotulavel {

       AGRICULTURA("Agricultura"),
       ARTESANATO("Artesanato"),
       OUTRA("Outra");

       private final String rotulo;

       Ocupacao(String rotulo) { this.rotulo = rotulo; }

       @Override
       public String getRotulo() { return rotulo; }
   }
   ```
   Constantes em `MAIUSCULAS_COM_SUBLINHADO`, sem acento. Quando existir
   classificação oficial (e-SUS, IBGE), use o vocabulário dela (ADR-0003).
2. Na entidade: `@Enumerated(EnumType.STRING)` e `@Column(length = N)` com N
   maior ou igual ao maior `name()`. Nunca use `ORDINAL`, que quebra ao
   reordenar.
3. Publique em `/api/metadados`: acrescente o campo em `MetadadosResponse`
   (camelCase, no singular, como `tipoFonteRenda`) e a linha correspondente
   `opcoes(Ocupacao.class)` em `MetadadosService`, na mesma posição.
4. Coluna nova no banco: siga a skill `nova-migracao-flyway`.

## Alterar um enum existente

| Mudança | O que fazer |
|---|---|
| Acrescentar valor | Só o enum. Confira se o `length` da coluna comporta o nome. |
| Mudar o rótulo | Só o enum. O valor gravado não muda. |
| Renomear valor | Migração com `UPDATE tabela SET col = 'NOVO' WHERE col = 'ANTIGO'`, e o enum no mesmo PR. Sem a migração, a entidade não carrega mais os registros antigos. |
| Remover valor | Migração que converte para outro valor ou para `NULL` (modelo: `V11__pessoa_serie_enum.sql`). |

Pré-cadastros guardam o JSON bruto (`pre_cadastro.payload`) e são convertidos
no service na hora de aprovar. Renomear um valor que o app envia faz os
pré-cadastros pendentes e os APKs antigos falharem. Nesse caso, aceite o nome
antigo por um tempo (`@JsonAlias` na constante, ou conversão no service) e
documente isso.

## Depois

- `mvn verify` (o teste de metadados, se houver, e os testes das rotas que
  usam o enum).
- Ajuste o **tipo** em `cadastro-familias-web/src/tipos/dominio.ts` (só a
  união de strings, nunca os rótulos) e, se o app envia esse campo,
  `cadastro-familias-app/src/dados/tipos.ts` e as opções da tela. A skill
  `mudanca-de-contrato`, na pasta que agrupa os repositórios, tem o passo a
  passo. Se os outros repositórios não estiverem disponíveis, escreva no PR
  exatamente o que mudou para a outra equipe.
- Todo campo coletado precisa aparecer em algum relatório ou filtro.

Pendências conhecidas: `Parentesco` ainda não está em `/api/metadados`, e
`Sexo` é `FEMININO`/`MASCULINO` aqui, enquanto web e app usam `F`/`M`.
