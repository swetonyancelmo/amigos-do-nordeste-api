---
name: novo-endpoint-api
description: Implementa uma rota nova ou altera uma existente na API Spring Boot do cadastro de famílias seguindo o padrão do projeto (pacote por módulo, controller → service → repositório, record de DTO, exceção própria mapeada no ManipuladorDeErros, Swagger em português, segurança ADMIN por padrão e teste MockMvc com o perfil test). Use sempre que a tarefa pedir endpoint, rota, controller, CRUD, filtro, paginação, relatório, regra de negócio no service ou "implementar a issue X" na API, mesmo que o usuário não diga "endpoint".
---

# Nova rota na API

Antes de começar: faça **só** o que a tarefa pede. O domínio é construído
pelo grupo por issues; não aproveite para criar o módulo inteiro. Se a rota
contraria alguma ADR de `docs/decisoes/`, pare e pergunte.

O módulo `municipio/` é o exemplo mais simples e completo do padrão. Leia-o
antes de escrever um módulo novo: `MunicipioController`, `MunicipioService`,
`dto/MunicipioRequisicao`, `dto/MunicipioResposta`,
`MunicipioNaoEncontradoException` e `src/test/.../municipio/MunicipioTest`.

## Estrutura de um módulo

```
br/org/amigosdonordeste/cadastro/<modulo>/
  <Entidade>.java               @Entity, Lombok (@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor)
  <Entidade>Repositorio.java    JpaRepository<Entidade, UUID>; consultas derivadas ou Specification
  <Entidade>Service.java        regra de negócio, @Transactional (readOnly nas leituras)
  <Entidade>Controller.java     @Tag, @RestController, @RequestMapping("/api/<plural-com-hifen>")
  dto/ (ou request/)            records de requisição (Bean Validation) e de resposta
  enums/                        listas fechadas (ver skill nova-lista-fechada)
  exception/                    exceções do módulo
```

Nomes em português: `Repositorio` (não `Repository`), `Requisicao`/`Resposta`
(alguns módulos antigos usam `Request`/`Response`; siga o vizinho quando mexer
num módulo existente). Rotas no plural com hífen: `/api/pre-cadastros`.

## Regras que não se negociam

1. **Entidade nunca sai na resposta.** Crie um `record` com um método
   `static de(Entidade)` para a conversão, como em `MunicipioResposta.de`.
2. **Validação no record de requisição** (`@NotBlank`, `@Size`, `@Pattern`)
   com mensagem em português, porque quem lê é a usuária. Use `@Valid
   @RequestBody` no controller. Campo vazio vindo de formulário vira `null` no
   service.
3. **Segurança:** toda rota nasce `ROLE_ADMIN` sem você fazer nada
   (`anyRequest().hasRole("ADMIN")`). **Não** mexa em `SegurancaConfig` para
   uma rota do painel. Rota para o aparelho da agente é exceção rara: exige
   entrada explícita em `SegurancaConfig`, `@PreAuthorize("hasRole('AGENTE')")`
   no método, e não pode devolver dado de família (ADR-0002). Confirme com o
   usuário antes de abrir qualquer rota nova para a agente ou para o público.
4. **Erros:** crie uma exceção específica (`XNaoEncontradaException`,
   `XJaCadastradoException`) com a mensagem em português e registre um
   `@ExceptionHandler` em `comum/ManipuladorDeErros` com o status certo
   (404, 409, 400). A resposta sai como `ErroResposta { em, status, message }`,
   e o web lê `message`.
5. **Swagger:** `@Operation(summary = "...")` em português em todo método. Em
   rota com respostas não óbvias, use também `@ApiResponses`, como em
   `PreCadastroController`. O Swagger é o contrato com o web.
6. **Listagem paginada** devolve `PaginaResposta<T>` (`itens`, `pagina`,
   `porPagina`, `total`, `totalPaginas`), nunca o `Page` do Spring. Filtros
   num record `XFiltroDTO` com os parâmetros opcionais e normalização de
   página e tamanho (veja `FamiliaFiltroDTO`), montados com `Specification`
   (veja `FamiliaEspecificacao`).
7. **Nada calculado é gravado.** Totais e idades são calculados na consulta ou
   no service (`dominio/Idade`).
8. **Família inativa** (`ativa = false`) não aparece em listagem, contagem nem
   relatório, a menos que a rota tenha um parâmetro explícito para isso.
9. **Log** nunca imprime CPF, telefone, senha nem token.

## Teste

Um arquivo `src/test/java/.../<modulo>/<Algo>Test.java` com
`@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")`. Siga
`MunicipioTest`:

- no `@BeforeEach`, limpe as tabelas **na ordem das FKs** (pré-cadastro →
  família → comunidade → município → usuário), porque o banco H2 é
  compartilhado entre as classes;
- crie um admin e gere o token com `jwt.gerarAcesso(id, email, Papel.ADMIN)`;
- cubra no mínimo: sem token → 401; caminho feliz; validação → 400; não
  encontrado → 404; regra de negócio específica da tarefa;
- use só dados fictícios ("Família de Teste", "Sítio de Teste").

```bash
mvn test -Dtest=NomeDoTeste
mvn verify
```

Se a rota precisa de tabela ou coluna nova, siga também a skill
`nova-migracao-flyway`. O H2 dos testes não roda a migração.

## Antes de dizer que terminou

- [ ] `mvn verify` passou
- [ ] a rota aparece certa no Swagger (`/swagger-ui.html`) se a API foi
      subida (skill `rodar-api-local`)
- [ ] o web ou o app consomem algo que mudou? Siga a skill
      `mudanca-de-contrato`, ou registre no PR o que a outra equipe precisa
      ajustar
- [ ] README (tabela "O que já existe") atualizado se a rota é nova
