package br.org.amigosdonordeste.cadastro.vulnerabilidade;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Sexo;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.function.Consumer;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A avaliação de vulnerabilidade pela API (ADR-0010): ficha, listagem com
 * filtro e ordem por estrato, relatório, metadados, acesso, e a prova de que
 * a base de conhecimento é dado: um UPDATE no banco muda o resultado na
 * próxima requisição, sem recompilar.
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VulnerabilidadeIntegracaoTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired AgenteRepositorio agentes;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;
    @Autowired TokenAgenteService tokens;
    @Autowired JdbcTemplate jdbc;

    private String bearerAdmin;
    private String bearerAgente;
    private Municipio municipioA;
    private Comunidade comunidadeA;
    private Comunidade comunidadeB;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        agentes.deleteAll();
        familias.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();
        usuarios.deleteAll();

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearerAdmin = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());

        String token = tokens.gerar();
        Agente agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setTokenHash(TokenAgenteService.hash(token));
        agente.setAtivo(true);
        agentes.save(agente);
        bearerAgente = "Bearer " + token;

        municipioA = municipios.save(Municipio.builder().nome("Município A").uf("PE").build());
        Municipio municipioB = municipios.save(Municipio.builder().nome("Município B").uf("PE").build());
        comunidadeA = comunidades.save(Comunidade.builder().municipio(municipioA).nome("Sítio A").build());
        comunidadeB = comunidades.save(Comunidade.builder().municipio(municipioB).nome("Sítio B").build());
    }

    // ------------------------------------------------------------------ apoio

    /** Família completa e sem nada precário (escore 0), ajustada pelo teste. */
    private Familia salvar(String responsavel, Comunidade comunidade, Consumer<Familia> ajuste) {
        Familia familia = Familia.builder()
                .comunidade(comunidade)
                .responsavelNome(responsavel)
                .temBanheiro(true)
                .escoamentoSanitario(EscoamentoSanitario.FOSSA_SEPTICA)
                .tratamentoAgua(TratamentoAgua.CLORADA)
                .faixaRenda(FaixaRenda.ATE_1_SALARIO)
                .numeroComodos(4)
                .build();
        familia.getAbastecimentoAgua().add(AbastecimentoAgua.CISTERNA);
        fonte(familia, TipoFonteRenda.TRABALHO_INFORMAL);
        pessoa(familia, 30);
        pessoa(familia, 32);
        ajuste.accept(familia);
        return familias.save(familia);
    }

    /** Família sem nada além do obrigatório: o caso das planilhas antigas. */
    private Familia salvarVazia(String responsavel, Comunidade comunidade) {
        return familias.save(Familia.builder().comunidade(comunidade).responsavelNome(responsavel).build());
    }

    private static void pessoa(Familia familia, int anos) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome("Membro Teste");
        pessoa.setSexo(Sexo.FEMININO);
        pessoa.setDataNascimento(LocalDate.now().minusYears(anos).minusDays(10));
        familia.adicionarPessoa(pessoa);
    }

    private static void fonte(Familia familia, TipoFonteRenda tipo) {
        FonteRenda fonte = new FonteRenda();
        fonte.setTipo(tipo);
        familia.adicionarFonteRenda(fonte);
    }

    private static void trocarFonte(Familia familia, TipoFonteRenda tipo) {
        familia.getFontesRenda().clear();
        fonte(familia, tipo);
    }

    /** 3 + 2 + 3 + 1 = 9: sem banheiro, adulto sem trabalho, 3 em 2 cômodos, idoso. */
    private Familia salvarR3(String responsavel, Comunidade comunidade) {
        return salvar(responsavel, comunidade, f -> {
            f.setTemBanheiro(false);
            trocarFonte(f, TipoFonteRenda.APOSENTADORIA);
            pessoa(f, 75);
            f.setNumeroComodos(2);
        });
    }

    /** 3 + 2 + 2 = 7: sem banheiro, sem trabalho, 2 em 2 cômodos. */
    private Familia salvarR2(String responsavel, Comunidade comunidade) {
        return salvar(responsavel, comunidade, f -> {
            f.setTemBanheiro(false);
            trocarFonte(f, TipoFonteRenda.BOLSA_FAMILIA);
            f.setNumeroComodos(2);
        });
    }

    /** Só saneamento: 3, sem risco identificado. */
    private Familia salvarSoSaneamento(String responsavel, Comunidade comunidade) {
        return salvar(responsavel, comunidade, f -> f.setEscoamentoSanitario(EscoamentoSanitario.CEU_ABERTO));
    }

    // ------------------------------------------------------------------ ficha

    @Test
    @DisplayName("a ficha traz estrato, escore e a explicação com cada sentinela que disparou")
    void fichaComExplicacao() throws Exception {
        Familia familia = salvarR3("Família Teste R3", comunidadeA);

        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.numeroComodos").value(2))
            .andExpect(jsonPath("$.vulnerabilidade.estrato").value("R3"))
            .andExpect(jsonPath("$.vulnerabilidade.rotulo").value("Maior necessidade de apoio"))
            .andExpect(jsonPath("$.vulnerabilidade.escore").value(9))
            .andExpect(jsonPath("$.vulnerabilidade.escoreMaximoAlcancavel").value(10))
            .andExpect(jsonPath("$.vulnerabilidade.sentinelasPresentes[*].codigo", containsInAnyOrder(
                    "BAIXAS_CONDICOES_SANEAMENTO", "DESEMPREGO", "MAIOR_DE_70_ANOS", "RELACAO_MORADOR_COMODO")))
            .andExpect(jsonPath("$.vulnerabilidade.sentinelasPresentes[?(@.codigo == 'RELACAO_MORADOR_COMODO')].pontos",
                    hasItem(3)))
            .andExpect(jsonPath("$.vulnerabilidade.sentinelasAusentes[*].codigo", containsInAnyOrder("MENOR_DE_SEIS_MESES")))
            .andExpect(jsonPath("$.vulnerabilidade.sentinelasIndeterminadas", hasSize(0)));
    }

    @Test
    @DisplayName("família importada sem dado nenhum sai DADOS_INSUFICIENTES, sem escore, com o que falta")
    void fichaSemDados() throws Exception {
        Familia familia = salvarVazia("Família Teste Vazia", comunidadeA);

        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.vulnerabilidade.estrato").value("DADOS_INSUFICIENTES"))
            .andExpect(jsonPath("$.vulnerabilidade.rotulo").value("Completar cadastro para avaliar"))
            .andExpect(jsonPath("$.vulnerabilidade.escore").value(nullValue()))
            .andExpect(jsonPath("$.vulnerabilidade.sentinelasAusentes", hasSize(0)))
            .andExpect(jsonPath("$.vulnerabilidade.camposFaltantes", hasItem("numeroComodos")))
            .andExpect(jsonPath("$.vulnerabilidade.camposFaltantes", hasItem("temBanheiro")))
            .andExpect(jsonPath("$.vulnerabilidade.camposFaltantes", hasItem("pessoas")));
    }

    // ------------------------------------------------ base de conhecimento é dado

    @Test
    @DisplayName("alterar um peso na base (UPDATE no banco) muda o resultado na próxima requisição, sem recompilar")
    void pesoAlteradoNoBanco() throws Exception {
        Familia familia = salvarSoSaneamento("Família Teste Peso", comunidadeA);

        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.vulnerabilidade.escore").value(3))
            .andExpect(jsonPath("$.vulnerabilidade.estrato").value("SEM_RISCO_IDENTIFICADO"));

        try {
            jdbc.update("UPDATE vulnerabilidade_sentinela SET pontos = 5 WHERE codigo = 'BAIXAS_CONDICOES_SANEAMENTO'");

            mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
                .andExpect(jsonPath("$.vulnerabilidade.escore").value(5))
                .andExpect(jsonPath("$.vulnerabilidade.estrato").value("R1"))
                .andExpect(jsonPath("$.vulnerabilidade.sentinelasPresentes[0].pontos").value(5));
        } finally {
            jdbc.update("UPDATE vulnerabilidade_sentinela SET pontos = 3 WHERE codigo = 'BAIXAS_CONDICOES_SANEAMENTO'");
        }
    }

    @Test
    @DisplayName("o rótulo de exibição é configurável na base; o código do estrato não muda")
    void rotuloConfiguravel() throws Exception {
        Familia familia = salvarR2("Família Teste Rótulo", comunidadeA);

        try {
            jdbc.update("UPDATE vulnerabilidade_estrato SET rotulo = 'Visitar nesta semana' WHERE codigo = 'R2'");

            mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAdmin))
                .andExpect(jsonPath("$.vulnerabilidade.estrato").value("R2"))
                .andExpect(jsonPath("$.vulnerabilidade.rotulo").value("Visitar nesta semana"));
            mvc.perform(get("/api/metadados"))
                .andExpect(jsonPath("$.estratoVulnerabilidade[?(@.valor == 'R2')].rotulo", hasItem("Visitar nesta semana")));
        } finally {
            jdbc.update("UPDATE vulnerabilidade_estrato SET rotulo = 'Necessidade intermediária de apoio' WHERE codigo = 'R2'");
        }
    }

    // --------------------------------------------------------------- listagem

    @Test
    @DisplayName("listagem: cada linha traz o estrato; filtro por estrato, inclusive DADOS_INSUFICIENTES")
    void listagemFiltraPorEstrato() throws Exception {
        Familia r3 = salvarR3("Ana Teste", comunidadeA);
        Familia r2 = salvarR2("Bia Teste", comunidadeA);
        salvar("Carla Teste", comunidadeB, f -> { });
        Familia vazia = salvarVazia("Dora Teste", comunidadeB);

        mvc.perform(get("/api/familias").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens", hasSize(4)))
            .andExpect(jsonPath("$.itens[0].vulnerabilidade.estrato").value("R3"))
            .andExpect(jsonPath("$.itens[0].vulnerabilidade.escore").value(9))
            .andExpect(jsonPath("$.itens[3].vulnerabilidade.estrato").value("DADOS_INSUFICIENTES"))
            .andExpect(jsonPath("$.itens[3].vulnerabilidade.escore").value(nullValue()));

        mvc.perform(get("/api/familias").param("estrato", "DADOS_INSUFICIENTES").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.itens[0].id").value(vazia.getId().toString()));

        mvc.perform(get("/api/familias").param("estrato", "R3").param("estrato", "R2")
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.total").value(2))
            .andExpect(jsonPath("$.itens[*].id", containsInAnyOrder(r3.getId().toString(), r2.getId().toString())));

        // combina com os outros filtros
        mvc.perform(get("/api/familias").param("estrato", "R3").param("comunidadeId", comunidadeB.getId().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    @DisplayName("ordenação por prioridade: R3, R2, R1, depois DADOS_INSUFICIENTES, e sem risco por último; paginada")
    void ordenacaoPorPrioridade() throws Exception {
        // nomes em ordem contrária à prioridade, para a ordem por nome não passar por acaso
        salvar("Ana Sem Risco", comunidadeA, f -> { });
        salvarVazia("Bia Incompleta", comunidadeA);
        salvar("Carla R1", comunidadeA, f -> {                 // 3 + 3 = 6
            f.setTemBanheiro(false);
            pessoa(f, 10);
            f.setNumeroComodos(2);
        });
        salvarR2("Dora R2", comunidadeA);
        salvarR3("Elza R3", comunidadeA);

        mvc.perform(get("/api/familias").param("ordenacao", "PRIORIDADE").header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.itens[*].responsavelNome").value(org.hamcrest.Matchers.contains(
                    "Elza R3", "Dora R2", "Carla R1", "Bia Incompleta", "Ana Sem Risco")));

        mvc.perform(get("/api/familias").param("ordenacao", "PRIORIDADE").param("porPagina", "2").param("pagina", "1")
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.total").value(5))
            .andExpect(jsonPath("$.totalPaginas").value(3))
            .andExpect(jsonPath("$.itens[*].responsavelNome").value(org.hamcrest.Matchers.contains(
                    "Carla R1", "Bia Incompleta")));
    }

    // ------------------------------------------------------------- cadastro

    @Test
    @DisplayName("número de cômodos entra pelo POST e volta na ficha; zero é recusado")
    void numeroComodosNoCadastro() throws Exception {
        String corpo = """
            {"comunidadeId": "%s", "responsavelNome": "Família Teste Cômodos", "numeroComodos": %s,
             "pessoas": [], "fontesRenda": []}
            """;

        mvc.perform(post("/api/familias").header("Authorization", bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo.formatted(comunidadeA.getId(), "3")))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.numeroComodos").value(3));

        mvc.perform(post("/api/familias").header("Authorization", bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content(corpo.formatted(comunidadeA.getId(), "0")))
            .andExpect(status().isBadRequest());
    }

    // -------------------------------------------------------------- relatório

    @Test
    @DisplayName("relatório: distribuição por estrato, por município e por comunidade, com DADOS_INSUFICIENTES e o que falta")
    void relatorio() throws Exception {
        salvarR3("Ana Teste", comunidadeA);
        salvarR2("Bia Teste", comunidadeA);
        salvarVazia("Carla Teste", comunidadeA);
        salvarVazia("Dora Teste", comunidadeB);
        Familia inativa = salvarR3("Elza Inativa", comunidadeB);
        inativa.inativar();
        familias.save(inativa);

        mvc.perform(get("/api/relatorios/vulnerabilidade").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalFamilias").value(4))
            .andExpect(jsonPath("$.escoreMaximoAlcancavel").value(10))
            .andExpect(jsonPath("$.distribuicao", hasSize(5)))
            .andExpect(jsonPath("$.distribuicao[0].estrato").value("R3"))
            .andExpect(jsonPath("$.distribuicao[0].valor").value(1))
            .andExpect(jsonPath("$.distribuicao[0].percentual").value(25.0))
            .andExpect(jsonPath("$.distribuicao[?(@.estrato == 'DADOS_INSUFICIENTES')].valor", hasItem(2)))
            .andExpect(jsonPath("$.distribuicao[?(@.estrato == 'DADOS_INSUFICIENTES')].rotulo",
                    hasItem("Completar cadastro para avaliar")))
            .andExpect(jsonPath("$.distribuicao[?(@.estrato == 'R1')].valor", hasItem(0)))
            .andExpect(jsonPath("$.porMunicipio", hasSize(2)))
            .andExpect(jsonPath("$.porMunicipio[0].municipioNome").value("Município A"))
            .andExpect(jsonPath("$.porMunicipio[0].totalFamilias").value(3))
            .andExpect(jsonPath("$.porComunidade", hasSize(2)))
            .andExpect(jsonPath("$.porComunidade[1].comunidadeNome").value("Sítio B"))
            .andExpect(jsonPath("$.porComunidade[1].distribuicao[?(@.estrato == 'DADOS_INSUFICIENTES')].valor", hasItem(1)))
            .andExpect(jsonPath("$.camposFaltantes[?(@.campo == 'numeroComodos')].familias", hasItem(2)))
            .andExpect(jsonPath("$.sentinelasNaoAvaliadas", hasSize(8)))
            // só contagem: nenhum nome de família no relatório
            .andExpect(jsonPath("$..responsavelNome").isEmpty());

        mvc.perform(get("/api/relatorios/vulnerabilidade").param("municipioId", municipioA.getId().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(jsonPath("$.totalFamilias").value(3))
            .andExpect(jsonPath("$.porComunidade", hasSize(1)));
    }

    @Test
    @DisplayName("escore só com login de administração: sem token é 401, token da agente é 403")
    void acesso() throws Exception {
        Familia familia = salvarR3("Família Teste Acesso", comunidadeA);

        mvc.perform(get("/api/relatorios/vulnerabilidade")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/relatorios/vulnerabilidade").header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/familias").param("ordenacao", "PRIORIDADE").header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/familias/" + familia.getId()).header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("metadados trazem os estratos com o rótulo da base, na ordem de prioridade")
    void metadados() throws Exception {
        mvc.perform(get("/api/metadados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estratoVulnerabilidade[*].valor").value(org.hamcrest.Matchers.contains(
                    "R3", "R2", "R1", "DADOS_INSUFICIENTES", "SEM_RISCO_IDENTIFICADO")))
            .andExpect(jsonPath("$.estratoVulnerabilidade[0].rotulo").value("Maior necessidade de apoio"));
    }

    // ------------------------------------------------- base em uso (transparência)

    @Test
    @DisplayName("base em uso: sentinelas com pontos, faixas, estratos com faixa de escore e o que fica de fora")
    void baseEmUso() throws Exception {
        mvc.perform(get("/api/vulnerabilidade/base").header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.escoreMaximoAlcancavel").value(10))
            .andExpect(jsonPath("$.sentinelas", hasSize(5)))
            .andExpect(jsonPath("$.sentinelas[?(@.codigo == 'BAIXAS_CONDICOES_SANEAMENTO')].pontos", hasItem(3)))
            .andExpect(jsonPath("$.sentinelas[?(@.codigo == 'BAIXAS_CONDICOES_SANEAMENTO')].criterio",
                    hasItem(org.hamcrest.Matchers.containsString("não tem banheiro"))))
            .andExpect(jsonPath("$.sentinelas[?(@.codigo == 'RELACAO_MORADOR_COMODO')].tipo", hasItem("FAIXA")))
            .andExpect(jsonPath("$.sentinelas[?(@.codigo == 'RELACAO_MORADOR_COMODO')].faixas[*].pontos",
                    containsInAnyOrder(0, 2, 3)))
            .andExpect(jsonPath("$.estratos[0].estrato").value("R3"))
            .andExpect(jsonPath("$.estratos[0].escoreMinimo").value(9))
            .andExpect(jsonPath("$.estratos[0].escoreMaximo").value(nullValue()))
            .andExpect(jsonPath("$.estratos[?(@.estrato == 'R2')].escoreMaximo", hasItem(8)))
            .andExpect(jsonPath("$.estratos[?(@.estrato == 'R1')].escoreMaximo", hasItem(6)))
            .andExpect(jsonPath("$.estratos[?(@.estrato == 'SEM_RISCO_IDENTIFICADO')].escoreMaximo", hasItem(4)))
            .andExpect(jsonPath("$.estratos[?(@.estrato == 'DADOS_INSUFICIENTES')].escoreMinimo", hasItem(nullValue())))
            .andExpect(jsonPath("$.sentinelasNaoAvaliadas", hasSize(8)));
    }

    @Test
    @DisplayName("base em uso mostra o peso do banco: ajustado lá, muda aqui")
    void baseMostraPesoDoBanco() throws Exception {
        try {
            jdbc.update("UPDATE vulnerabilidade_sentinela SET pontos = 4 WHERE codigo = 'DESEMPREGO'");
            mvc.perform(get("/api/vulnerabilidade/base").header("Authorization", bearerAdmin))
                .andExpect(jsonPath("$.sentinelas[?(@.codigo == 'DESEMPREGO')].pontos", hasItem(4)))
                .andExpect(jsonPath("$.escoreMaximoAlcancavel").value(12));
        } finally {
            jdbc.update("UPDATE vulnerabilidade_sentinela SET pontos = 2 WHERE codigo = 'DESEMPREGO'");
        }
    }

    @Test
    @DisplayName("base em uso é só da administração: sem token 401, token da agente 403")
    void baseSoAdmin() throws Exception {
        mvc.perform(get("/api/vulnerabilidade/base")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/vulnerabilidade/base").header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
    }
}
