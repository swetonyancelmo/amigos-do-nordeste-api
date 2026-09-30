package br.org.amigosdonordeste.cadastro.pessoa;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.FonteRendaRepositorio;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints de pessoa: GET /api/pessoas, GET/PUT/DELETE /api/pessoas/{id} e
 * POST /api/familias/{familiaId}/pessoas.
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PessoaTest {

    @Autowired MockMvc mvc;
    @Autowired FamiliaRepositorio familias;
    @Autowired PessoaRepositorio pessoas;
    @Autowired FonteRendaRepositorio fontes;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    private final LocalDate hoje = LocalDate.now();
    private String bearerAdmin;
    private Municipio municipio;
    private Comunidade comunidade;
    private Familia familia;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
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

        municipio = municipios.save(Municipio.builder().nome("Município Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio Teste").build());
        familia = familias.save(Familia.builder().comunidade(comunidade).responsavelNome("Responsável Teste").build());
    }

    // ---------- apoio ----------

    private MockHttpServletRequestBuilder autenticado(MockHttpServletRequestBuilder req) {
        return req.header("Authorization", bearerAdmin);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder req, String corpo) {
        return autenticado(req).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static String corpo(String nome, boolean incompleto, String dataNascimento,
                                Integer idadeEstimada, String idadeEstimadaEm) {
        return """
            {
              "nome": %s,
              "cadastroIncompleto": %s,
              "sexo": "FEMININO",
              "dataNascimento": %s,
              "idadeEstimada": %s,
              "idadeEstimadaEm": %s,
              "parentesco": "FILHO",
              "estuda": true,
              "serie": "ANO_7",
              "tamanhoRoupa": "INFANTIL_12",
              "numeroCalcado": "32/33",
              "gestante": false,
              "observacoes": null
            }
            """.formatted(
                texto(nome), incompleto, texto(dataNascimento), idadeEstimada, texto(idadeEstimadaEm));
    }

    private static String texto(String valor) {
        return valor == null ? "null" : "\"" + valor + "\"";
    }

    private Pessoa membro(Familia daFamilia, String nome, LocalDate nascimento) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setDataNascimento(nascimento);
        daFamilia.adicionarPessoa(pessoa);
        return pessoa;
    }

    private Pessoa membroEstimado(Familia daFamilia, String nome, int idadeEstimada, LocalDate estimadaEm) {
        Pessoa pessoa = new Pessoa();
        pessoa.setNome(nome);
        pessoa.setIdadeEstimada(idadeEstimada);
        pessoa.setIdadeEstimadaEm(estimadaEm);
        daFamilia.adicionarPessoa(pessoa);
        return pessoa;
    }

    // ---------- criação e validação ----------

    @Test
    @DisplayName("POST cria pessoa na família e devolve 201 com a ficha completa")
    void criaPessoa() throws Exception {
        String nascimento = hoje.minusYears(12).toString();
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo("Pessoa Teste", false, nascimento, null, null)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.nome").value("Pessoa Teste"))
            .andExpect(jsonPath("$.cadastroIncompleto").value(false))
            .andExpect(jsonPath("$.dataNascimento").value(nascimento))
            .andExpect(jsonPath("$.idade").value(12))
            .andExpect(jsonPath("$.numeroCalcado").value("32/33"))
            .andExpect(jsonPath("$.familia.id").value(familia.getId().toString()))
            .andExpect(jsonPath("$.familia.responsavelNome").value("Responsável Teste"))
            .andExpect(jsonPath("$.comunidade.nome").value("Sítio Teste"))
            .andExpect(jsonPath("$.municipio.nome").value("Município Teste"));
    }

    @Test
    @DisplayName("pessoa sem nome com cadastroIncompleto = true é aceita")
    void semNomeIncompletoAceita() throws Exception {
        for (String nome : new String[] {null, "", "  "}) {
            mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                    corpo(nome, true, null, null, null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cadastroIncompleto").value(true))
                .andExpect(jsonPath("$.idade").value(nullValue()));
        }
    }

    @Test
    @DisplayName("pessoa sem nome com cadastroIncompleto = false devolve 400")
    void semNomeCompletoRecusada() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo(null, false, hoje.minusYears(5).toString(), null, null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("nome")));
    }

    @Test
    @DisplayName("idadeEstimada sem idadeEstimadaEm devolve 400 (não preenche com hoje)")
    void estimativaSemDataRecusada() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo("Pessoa Teste", false, null, 30, null)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("idadeEstimadaEm")));
        assertTrue(pessoas.findAll().isEmpty());
    }

    @Test
    @DisplayName("dataNascimento e idadeEstimada juntos devolvem 400")
    void dataEEstimativaJuntasRecusadas() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo("Pessoa Teste", false, hoje.minusYears(30).toString(), 30, hoje.toString())))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").exists());
    }

    @Test
    @DisplayName("dataNascimento no futuro devolve 400")
    void nascimentoNoFuturoRecusado() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo("Pessoa Teste", false, hoje.plusDays(1).toString(), null, null)))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem data e sem estimativa é aceito: idade null")
    void semIdadeAlgumaAceita() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()),
                corpo("Pessoa Teste", false, null, null, null)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.idade").value(nullValue()));
    }

    @Test
    @DisplayName("texto livre em lista fechada e calçado fora da lista devolvem 400 no formato padrão")
    void listaFechadaRecusaTextoLivre() throws Exception {
        String sexoLivre = corpo("Pessoa Teste", false, null, null, null).replace("\"FEMININO\"", "\"mulher\"");
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()), sexoLivre))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("sexo")));

        String calcadoLivre = corpo("Pessoa Teste", false, null, null, null).replace("\"32/33\"", "\"33\"");
        mvc.perform(json(post("/api/familias/{id}/pessoas", familia.getId()), calcadoLivre))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("numeroCalcado")));
    }

    @Test
    @DisplayName("POST em família que não existe devolve 404")
    void familiaInexistente() throws Exception {
        mvc.perform(json(post("/api/familias/{id}/pessoas", UUID.randomUUID()),
                corpo("Pessoa Teste", false, null, null, null)))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.message").exists());
    }

    // ---------- leitura e edição ----------

    @Test
    @DisplayName("GET e PUT por id: edita e devolve 200 com a ficha; id inexistente é 404")
    void buscaEEdita() throws Exception {
        Pessoa pessoa = membro(familia, "Pessoa Antes", hoje.minusYears(20));
        familias.save(familia);
        UUID id = pessoas.findAll().get(0).getId();

        mvc.perform(autenticado(get("/api/pessoas/{id}", id)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value(pessoa.getNome()))
            .andExpect(jsonPath("$.idade").value(20))
            .andExpect(jsonPath("$.comunidade.id").value(comunidade.getId().toString()));

        LocalDate estimadaEm = hoje.minusYears(2);
        mvc.perform(json(put("/api/pessoas/{id}", id),
                corpo("Pessoa Depois", false, null, 40, estimadaEm.toString())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Pessoa Depois"))
            .andExpect(jsonPath("$.dataNascimento").value(nullValue()))
            .andExpect(jsonPath("$.idadeEstimada").value(40))
            .andExpect(jsonPath("$.idade").value(42))
            .andExpect(jsonPath("$.familia.id").value(familia.getId().toString()));

        mvc.perform(autenticado(get("/api/pessoas/{id}", UUID.randomUUID())))
            .andExpect(status().isNotFound());
    }

    // ---------- listagem ----------

    @Test
    @DisplayName("busca por \"jose\" encontra \"José\"")
    void buscaIgnoraAcento() throws Exception {
        membro(familia, "José Teste", hoje.minusYears(30));
        membro(familia, "Maria Teste", hoje.minusYears(30));
        familias.save(familia);

        for (String termo : new String[] {"jose", "JOSÉ", "Jose teste"}) {
            mvc.perform(autenticado(get("/api/pessoas").param("nome", termo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens", hasSize(1)))
                .andExpect(jsonPath("$.itens[0].nome").value("José Teste"))
                .andExpect(jsonPath("$.itens[0].familia.responsavelNome").value("Responsável Teste"))
                .andExpect(jsonPath("$.itens[0].comunidade.nome").value("Sítio Teste"))
                .andExpect(jsonPath("$.itens[0].municipio.nome").value("Município Teste"));
        }
    }

    @Test
    @DisplayName("faixaEtaria é filtrada no banco: contagem e paginação certas")
    void filtraFaixaEtariaNoBanco() throws Exception {
        // ATE_12 — 5 pessoas, por data e por estimativa, incluindo as bordas
        membro(familia, "Crianca 1", hoje.minusYears(3));
        membro(familia, "Crianca 2", hoje.minusYears(13).plusDays(1));   // faz 13 amanhã: ainda 12
        membro(familia, "Crianca 3", hoje);                                // nasceu hoje: 0
        membroEstimado(familia, "Crianca 4", 10, hoje.minusYears(2));     // 10 + 2 = 12
        membroEstimado(familia, "Crianca 5", 5, hoje);                    // 5
        // DE_13_A_59
        membro(familia, "Adulto 1", hoje.minusYears(13));                 // faz 13 hoje
        membroEstimado(familia, "Adulto 2", 10, hoje.minusYears(3));      // 10 + 3 = 13: envelheceu
        membro(familia, "Adulto 3", hoje.minusYears(60).plusDays(1));     // ainda 59
        // DE_60_OU_MAIS
        membro(familia, "Idoso 1", hoje.minusYears(60));
        membroEstimado(familia, "Idoso 2", 58, hoje.minusYears(2));       // 58 + 2 = 60
        // sem idade: não entra em faixa nenhuma
        membro(familia, "Sem Idade", null);
        familias.save(familia);

        // criança de família inativa não conta
        Familia inativa = Familia.builder().comunidade(comunidade).responsavelNome("Outra Responsável").build();
        membro(inativa, "Crianca Inativa", hoje.minusYears(5));
        inativa.inativar();
        familias.save(inativa);

        mvc.perform(autenticado(get("/api/pessoas")
                .param("faixaEtaria", "ATE_12").param("tamanho", "2")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(5))
            .andExpect(jsonPath("$.totalPaginas").value(3))
            .andExpect(jsonPath("$.porPagina").value(2))
            .andExpect(jsonPath("$.itens", hasSize(2)));

        mvc.perform(autenticado(get("/api/pessoas")
                .param("faixaEtaria", "ATE_12").param("tamanho", "2").param("pagina", "2")))
            .andExpect(jsonPath("$.pagina").value(2))
            .andExpect(jsonPath("$.itens", hasSize(1)));

        mvc.perform(autenticado(get("/api/pessoas").param("faixaEtaria", "DE_13_A_59")))
            .andExpect(jsonPath("$.total").value(3));

        mvc.perform(autenticado(get("/api/pessoas").param("faixaEtaria", "DE_60_OU_MAIS")))
            .andExpect(jsonPath("$.total").value(2));

        // sem filtro: todo mundo das famílias ativas, com padrão de 20 por página
        mvc.perform(autenticado(get("/api/pessoas")))
            .andExpect(jsonPath("$.total").value(11))
            .andExpect(jsonPath("$.porPagina").value(20));

        // e a idade da estimativa vem marcada como estimada
        mvc.perform(autenticado(get("/api/pessoas").param("nome", "Idoso 2")))
            .andExpect(jsonPath("$.itens[0].idade").value(60))
            .andExpect(jsonPath("$.itens[0].idadeEstimada").value(true));
        mvc.perform(autenticado(get("/api/pessoas").param("nome", "Idoso 1")))
            .andExpect(jsonPath("$.itens[0].idadeEstimada").value(false));
    }

    @Test
    @DisplayName("filtros de família, comunidade, município, incompleto e estuda se combinam")
    void filtrosCombinaveis() throws Exception {
        Municipio outroMunicipio = municipios.save(Municipio.builder().nome("Outro Município").uf("PE").build());
        Comunidade outraComunidade = comunidades.save(
                Comunidade.builder().municipio(outroMunicipio).nome("Outro Sítio").build());
        Familia outra = Familia.builder().comunidade(outraComunidade).responsavelNome("Outra Responsável").build();

        Pessoa estuda = membro(familia, "Estudante", hoje.minusYears(10));
        estuda.setEstuda(true);
        Pessoa incompleta = membro(familia, null, null);
        incompleta.setCadastroIncompleto(true);
        membro(outra, "De Fora", hoje.minusYears(10));
        familias.save(familia);
        familias.save(outra);

        mvc.perform(autenticado(get("/api/pessoas").param("familiaId", familia.getId().toString())))
            .andExpect(jsonPath("$.total").value(2));
        mvc.perform(autenticado(get("/api/pessoas").param("comunidadeId", outraComunidade.getId().toString())))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.itens[0].nome").value("De Fora"));
        mvc.perform(autenticado(get("/api/pessoas").param("municipioId", municipio.getId().toString())))
            .andExpect(jsonPath("$.total").value(2));
        mvc.perform(autenticado(get("/api/pessoas").param("cadastroIncompleto", "true")))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.itens[0].cadastroIncompleto").value(true));
        mvc.perform(autenticado(get("/api/pessoas").param("estuda", "true")
                .param("municipioId", municipio.getId().toString())))
            .andExpect(jsonPath("$.total").value(1))
            .andExpect(jsonPath("$.itens[0].nome").value("Estudante"));

        mvc.perform(autenticado(get("/api/pessoas").param("faixaEtaria", "QUALQUER")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message", containsString("faixaEtaria")));
    }

    // ---------- remoção ----------

    @Test
    @DisplayName("apagar pessoa com fonte de renda deixa a fonte na família com pessoaId nulo")
    void apagarMantemFonteDaFamilia() throws Exception {
        Pessoa pessoa = membro(familia, "Pessoa Com Renda", hoje.minusYears(40));
        FonteRenda fonte = FonteRenda.builder().tipo(TipoFonteRenda.BOLSA_FAMILIA).build();
        familia.adicionarFonteRenda(fonte);
        fonte.setPessoa(pessoa);
        familias.save(familia);

        UUID pessoaId = pessoas.findAll().get(0).getId();
        UUID fonteId = fontes.findAll().get(0).getId();

        mvc.perform(autenticado(delete("/api/pessoas/{id}", pessoaId)))
            .andExpect(status().isNoContent());

        assertFalse(pessoas.existsById(pessoaId));
        FonteRenda depois = fontes.findById(fonteId).orElseThrow();
        assertNull(depois.getPessoa());
        assertTrue(fontes.findByFamiliaId(familia.getId()).size() == 1);

        mvc.perform(autenticado(delete("/api/pessoas/{id}", pessoaId)))
            .andExpect(status().isNotFound());
    }

    // ---------- segurança ----------

    @Test
    @DisplayName("as cinco rotas sem token devolvem 401")
    void semTokenDevolve401() throws Exception {
        UUID id = UUID.randomUUID();
        String corpo = corpo("Pessoa Teste", false, null, null, null);

        mvc.perform(get("/api/pessoas")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/pessoas/{id}", id)).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/familias/{id}/pessoas", familia.getId())
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/pessoas/{id}", id)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/pessoas/{id}", id)).andExpect(status().isUnauthorized());
    }
}
