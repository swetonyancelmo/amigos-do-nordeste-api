package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.Pessoa;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Parentesco;
import br.org.amigosdonordeste.cadastro.pessoa.enums.Serie;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem (issue de aprovar/devolver):
 *  - aprovar cria a familia pelo FamiliaService e preenche familia_id;
 *  - o que a agente coletou (nome, telefone, pessoas) e o que a revisora
 *    completou (agua, esgoto, renda e os complementos por pessoa) chegam
 *    juntos na familia criada;
 *  - aprovar sem corpo nenhum funciona;
 *  - aprovar duas vezes nao cria duas familias (409 na segunda);
 *  - pessoas[].indice fora da faixa ou repetido e 400, e nada e criado;
 *  - devolver sem motivo e 400; com motivo, fica DEVOLVIDO e nao aprova mais;
 *  - enquanto PENDENTE nao existe familia — e por isso nao entra em
 *    relatorio, contagem nem mapa, que leem so a tabela familia.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreCadastroAvaliacaoTest {

    @Autowired MockMvc mvc;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired FamiliaRepositorio familias;
    @Autowired AgenteRepositorio agentes;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;
    @Autowired TransactionTemplate transacao;

    private String bearerAdmin;
    private Agente agente;
    private Comunidade comunidade;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        familias.deleteAll();
        agentes.deleteAll();
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

        agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setTokenHash("hash-qualquer");
        agente = agentes.save(agente);

        Municipio municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio de Teste").build());
    }

    @AfterEach
    void limpar() {
        preCadastros.deleteAll();
        familias.deleteAll();
    }

    /** Pre-cadastro como o aparelho manda: responsavel + duas pessoas com nome, sexo e idade. */
    private PreCadastro salvarPendente(Comunidade comunidade) {
        PreCadastro p = new PreCadastro();
        p.setId(UUID.randomUUID());
        p.setAgente(agente);
        p.setComunidade(comunidade);
        p.setPayload("""
            {
              "id": "%s",
              "responsavelNome": "Responsável de Teste",
              "telefone": "87999990000",
              "comunidadeNome": "Sítio de Teste",
              "pontoReferencia": "Perto da escola",
              "criadoEm": "2026-09-12T09:12:00Z",
              "pessoas": [
                { "id": "%s", "nome": "Responsável de Teste", "cadastroIncompleto": false, "sexo": "FEMININO",
                  "idadeEstimada": 34, "idadeEstimadaEm": "2026-09-12" },
                { "id": "%s", "nome": "Criança de Teste", "cadastroIncompleto": false, "sexo": "MASCULINO",
                  "dataNascimento": "2018-03-05" }
              ]
            }
            """.formatted(p.getId(), UUID.randomUUID(), UUID.randomUUID()));
        return preCadastros.save(p);
    }

    private MockHttpServletRequestBuilder aprovar(UUID id, String corpo) {
        MockHttpServletRequestBuilder req = post("/api/pre-cadastros/" + id + "/aprovar")
            .header("Authorization", bearerAdmin);
        return corpo == null ? req : req.contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private MockHttpServletRequestBuilder devolver(UUID id, String corpo) {
        return post("/api/pre-cadastros/" + id + "/devolver")
            .header("Authorization", bearerAdmin)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo);
    }

    private static final String COMPLEMENTO = """
        {
          "abastecimentoAgua": ["CISTERNA"],
          "escoamentoSanitario": "FOSSA_RUDIMENTAR",
          "tratamentoAgua": "SEM_TRATAMENTO",
          "temBanheiro": true,
          "fontesRenda": [ { "tipo": "BOLSA_FAMILIA", "pessoaIndice": null, "faixa": "ATE_1_SALARIO" } ],
          "pessoas": [
            { "indice": 0, "parentesco": "RESPONSAVEL", "estuda": false,
              "serie": null, "tamanhoRoupa": "ADULTO_G", "numeroCalcado": "38/39", "gestante": false },
            { "indice": 1, "parentesco": "FILHO", "estuda": true,
              "serie": "PRE", "tamanhoRoupa": "INFANTIL_8", "numeroCalcado": "30/31", "gestante": false }
          ]
        }
        """;

    @Test
    @DisplayName("enquanto pendente não existe família: relatório, contagem e mapa não o enxergam")
    void pendenteNaoViraFamilia() {
        salvarPendente(comunidade);
        assertEquals(0, familias.count());
    }

    @Test
    @DisplayName("aprovar cria a família com o coletado + o complemento e preenche familia_id")
    void aprovarCriaFamiliaCompleta() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        String resposta = mvc.perform(aprovar(pendente.getId(), COMPLEMENTO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.comunidadeId").value(comunidade.getId().toString()))
            .andExpect(jsonPath("$.responsavelNome").value("Responsável de Teste"))
            .andExpect(jsonPath("$.telefone").value("87999990000"))
            .andExpect(jsonPath("$.pontoReferencia").value("Perto da escola"))
            .andExpect(jsonPath("$.temBanheiro").value(true))
            .andExpect(jsonPath("$.escoamentoSanitario").value("FOSSA_RUDIMENTAR"))
            .andExpect(jsonPath("$.abastecimentoAgua[0]").value("CISTERNA"))
            .andExpect(jsonPath("$.pessoas", hasSize(2)))
            .andExpect(jsonPath("$.fontesRenda", hasSize(1)))
            .andExpect(jsonPath("$.fontesRenda[0].tipo").value("BOLSA_FAMILIA"))
            .andExpect(jsonPath("$.totais.totalPessoas").value(2))
            .andReturn().getResponse().getContentAsString();
        UUID familiaId = UUID.fromString(JsonPath.read(resposta, "$.id"));

        PreCadastro avaliado = preCadastros.findById(pendente.getId()).orElseThrow();
        assertEquals(SituacaoPreCadastro.APROVADO, avaliado.getSituacao());
        assertNotNull(avaliado.getAvaliadoEm());
        assertEquals(1, familias.count());

        transacao.executeWithoutResult(t -> {
            Familia familia = familias.findById(familiaId).orElseThrow();
            assertEquals(familiaId, preCadastros.findById(pendente.getId()).orElseThrow().getFamilia().getId());
            assertEquals(EscoamentoSanitario.FOSSA_RUDIMENTAR, familia.getEscoamentoSanitario());
            assertTrue(familia.getAbastecimentoAgua().contains(AbastecimentoAgua.CISTERNA));
            assertEquals(TipoFonteRenda.BOLSA_FAMILIA, familia.getFontesRenda().iterator().next().getTipo());

            // Pessoas: o coletado (nome, sexo, idade) e o complemento (roupa, calcado...) juntos.
            List<Pessoa> pessoas = familia.getPessoas().stream()
                .sorted(Comparator.comparing(Pessoa::getNome)).toList();
            Pessoa crianca = pessoas.get(0);
            assertEquals("Criança de Teste", crianca.getNome());
            assertEquals(Parentesco.FILHO, crianca.getParentesco());
            assertEquals(Serie.PRE, crianca.getSerie());
            assertEquals(TamanhoRoupa.INFANTIL_8, crianca.getTamanhoRoupa());
            assertEquals("30/31", crianca.getNumeroCalcado());
            assertEquals(Boolean.TRUE, crianca.getEstuda());
            Pessoa responsavel = pessoas.get(1);
            assertEquals(34, responsavel.getIdadeEstimada());
            assertEquals(Parentesco.RESPONSAVEL, responsavel.getParentesco());
            assertEquals(TamanhoRoupa.ADULTO_G, responsavel.getTamanhoRoupa());
            assertEquals("38/39", responsavel.getNumeroCalcado());
        });
    }

    @Test
    @DisplayName("aprovar sem corpo nenhum funciona")
    void aprovarSemComplemento() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(aprovar(pendente.getId(), null))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.pessoas", hasSize(2)))
            .andExpect(jsonPath("$.fontesRenda", hasSize(0)));

        assertEquals(1, familias.count());
        assertEquals(SituacaoPreCadastro.APROVADO, preCadastros.findById(pendente.getId()).orElseThrow().getSituacao());
    }

    @Test
    @DisplayName("aprovar duas vezes não cria duas famílias")
    void aprovarDuasVezes() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(aprovar(pendente.getId(), "{}")).andExpect(status().isCreated());
        mvc.perform(aprovar(pendente.getId(), "{}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").isString());

        assertEquals(1, familias.count());
    }

    @Test
    @DisplayName("pessoas[].indice fora da faixa é 400 e nada é criado")
    void indiceForaDaFaixaE400() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(aprovar(pendente.getId(), """
                { "pessoas": [ { "indice": 2, "tamanhoRoupa": "ADULTO_M" } ] }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());

        assertEquals(0, familias.count());
        assertEquals(SituacaoPreCadastro.PENDENTE, preCadastros.findById(pendente.getId()).orElseThrow().getSituacao());
    }

    @Test
    @DisplayName("pessoas[].indice repetido é 400")
    void indiceRepetidoE400() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(aprovar(pendente.getId(), """
                { "pessoas": [ { "indice": 0, "tamanhoRoupa": "ADULTO_M" }, { "indice": 0, "tamanhoRoupa": "ADULTO_G" } ] }
                """))
            .andExpect(status().isBadRequest());
        assertEquals(0, familias.count());
    }

    @Test
    @DisplayName("null dentro de pessoas[] ou fontesRenda[] é 400, não 500")
    void nullNasListasE400() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(aprovar(pendente.getId(), "{ \"pessoas\": [ null ] }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());
        mvc.perform(aprovar(pendente.getId(), "{ \"fontesRenda\": [ null ] }"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());

        assertEquals(0, familias.count());
        assertEquals(SituacaoPreCadastro.PENDENTE, preCadastros.findById(pendente.getId()).orElseThrow().getSituacao());
    }

    @Test
    @DisplayName("sem comunidade reconhecida, aprovar exige comunidadeId no corpo")
    void semComunidadeExigeComunidadeNoCorpo() throws Exception {
        PreCadastro pendente = salvarPendente(null);

        mvc.perform(aprovar(pendente.getId(), "{}")).andExpect(status().isBadRequest());
        assertEquals(0, familias.count());

        mvc.perform(aprovar(pendente.getId(), "{ \"comunidadeId\": \"" + comunidade.getId() + "\" }"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.comunidadeId").value(comunidade.getId().toString()));
    }

    @Test
    @DisplayName("devolver sem motivo é 400")
    void devolverSemMotivoE400() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(devolver(pendente.getId(), "{}")).andExpect(status().isBadRequest());
        mvc.perform(devolver(pendente.getId(), "{ \"motivo\": \"   \" }")).andExpect(status().isBadRequest());

        assertEquals(SituacaoPreCadastro.PENDENTE, preCadastros.findById(pendente.getId()).orElseThrow().getSituacao());
    }

    @Test
    @DisplayName("devolver guarda o motivo, e um devolvido não pode mais ser aprovado")
    void devolverComMotivo() throws Exception {
        PreCadastro pendente = salvarPendente(comunidade);

        mvc.perform(devolver(pendente.getId(), "{ \"motivo\": \"Faltou a idade da criança.\" }"))
            .andExpect(status().isNoContent());

        PreCadastro devolvido = preCadastros.findById(pendente.getId()).orElseThrow();
        assertEquals(SituacaoPreCadastro.DEVOLVIDO, devolvido.getSituacao());
        assertEquals("Faltou a idade da criança.", devolvido.getMotivoDevolucao());
        assertNotNull(devolvido.getAvaliadoEm());

        mvc.perform(aprovar(pendente.getId(), "{}")).andExpect(status().isConflict());
        mvc.perform(devolver(pendente.getId(), "{ \"motivo\": \"de novo\" }")).andExpect(status().isConflict());
        assertEquals(0, familias.count());
    }

    @Test
    @DisplayName("pré-cadastro que não existe é 404")
    void naoEncontradoE404() throws Exception {
        mvc.perform(aprovar(UUID.randomUUID(), "{}")).andExpect(status().isNotFound());
        mvc.perform(devolver(UUID.randomUUID(), "{ \"motivo\": \"x\" }")).andExpect(status().isNotFound());
    }
}
