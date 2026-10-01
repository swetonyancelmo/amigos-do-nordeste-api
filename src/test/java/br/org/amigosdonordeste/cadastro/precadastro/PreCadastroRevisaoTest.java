package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import com.fasterxml.jackson.databind.ObjectMapper;
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

import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Auditoria de integracao:
 *  - API-05: GET /api/pre-cadastros/{id} traz as pessoas com o indice que a
 *    aprovacao usa;
 *  - SYNC-01: a agente corrige um DEVOLVIDO e reenvia o mesmo id — ele volta
 *    para a fila com o payload novo, em vez de virar JA_RECEBIDO e sumir;
 *  - SYNC-02: GET /api/pre-cadastros/situacao devolve ao aparelho so o que
 *    ELE enviou, so id/situacao/motivo.
 *
 * Dados ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreCadastroRevisaoTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired AgenteRepositorio agentes;
    @Autowired FamiliaRepositorio familias;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired TokenAgenteService tokens;
    @Autowired JwtService jwt;

    private String bearerAgente;
    private String bearerOutraAgente;
    private String bearerAdmin;
    private Comunidade comunidade;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        familias.deleteAll();
        agentes.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();
        usuarios.deleteAll();

        bearerAgente = "Bearer " + salvarAgente("Agente de Teste");
        bearerOutraAgente = "Bearer " + salvarAgente("Outra Agente de Teste");

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearerAdmin = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());

        Municipio municipio = municipios.save(Municipio.builder().nome("Município de Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder().municipio(municipio).nome("Sítio de Teste").build());
    }

    @AfterEach
    void limpar() {
        preCadastros.deleteAll();
    }

    private String salvarAgente(String nome) {
        String token = tokens.gerar();
        Agente agente = new Agente();
        agente.setNome(nome);
        agente.setTokenHash(TokenAgenteService.hash(token));
        agentes.save(agente);
        return token;
    }

    private String payload(UUID id, String responsavel) {
        return """
            {
              "id": "%s",
              "responsavelNome": "%s",
              "telefone": "87999990000",
              "comunidadeId": "%s",
              "comunidadeNome": "Sítio de Teste",
              "pontoReferencia": "Perto da escola",
              "criadoEm": "2026-09-12T09:12:00Z",
              "pessoas": [
                { "id": "%s", "nome": "Criança de Teste", "cadastroIncompleto": false, "sexo": "F",
                  "idadeEstimada": 4, "idadeEstimadaEm": "2026-09-12" },
                { "id": "%s", "nome": null, "cadastroIncompleto": true }
              ]
            }
            """.formatted(id, responsavel, comunidade.getId(), UUID.randomUUID(), UUID.randomUUID());
    }

    private void enviar(String bearer, String corpo, String resultadoEsperado) throws Exception {
        mvc.perform(post("/api/pre-cadastros").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value(resultadoEsperado));
    }

    private void devolver(UUID id, String motivo) throws Exception {
        mvc.perform(post("/api/pre-cadastros/" + id + "/devolver").header("Authorization", bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"" + motivo + "\"}"))
            .andExpect(status().isNoContent());
    }

    // ------------------------------------------------------------ API-05

    @Test
    @DisplayName("detalhe traz o que a agente coletou, com o indice e a idade de cada pessoa")
    void detalhe() throws Exception {
        UUID id = UUID.randomUUID();
        enviar(bearerAgente, payload(id, "Responsável de Teste"), "ACEITO");

        mvc.perform(get("/api/pre-cadastros/" + id).header("Authorization", bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("PENDENTE"))
            .andExpect(jsonPath("$.agenteNome").value("Agente de Teste"))
            .andExpect(jsonPath("$.responsavelNome").value("Responsável de Teste"))
            .andExpect(jsonPath("$.comunidadeId").value(comunidade.getId().toString()))
            .andExpect(jsonPath("$.criadoEm").isNotEmpty())
            .andExpect(jsonPath("$.pessoas", hasSize(2)))
            .andExpect(jsonPath("$.pessoas[0].indice").value(0))
            .andExpect(jsonPath("$.pessoas[0].sexo").value("FEMININO"))
            .andExpect(jsonPath("$.pessoas[0].idade").isNumber())
            .andExpect(jsonPath("$.pessoas[1].indice").value(1))
            .andExpect(jsonPath("$.pessoas[1].cadastroIncompleto").value(true));
    }

    @Test
    @DisplayName("detalhe: 404 se nao existe e 403 para o token da agente")
    void detalheAcesso() throws Exception {
        mvc.perform(get("/api/pre-cadastros/" + UUID.randomUUID()).header("Authorization", bearerAdmin))
            .andExpect(status().isNotFound());

        UUID id = UUID.randomUUID();
        enviar(bearerAgente, payload(id, "Responsável de Teste"), "ACEITO");
        mvc.perform(get("/api/pre-cadastros/" + id).header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
    }

    // ------------------------------------------------------------ SYNC-01

    @Test
    @DisplayName("devolvido reenviado pela mesma agente volta a PENDENTE com o payload novo")
    void reenvioDeDevolvido() throws Exception {
        UUID id = UUID.randomUUID();
        enviar(bearerAgente, payload(id, "Nome Errado"), "ACEITO");
        devolver(id, "Confira o nome da responsável.");

        enviar(bearerAgente, payload(id, "Nome Corrigido"), "ACEITO");

        PreCadastro salvo = preCadastros.findById(id).orElseThrow();
        assertEquals(SituacaoPreCadastro.PENDENTE, salvo.getSituacao());
        assertNull(salvo.getMotivoDevolucao());
        assertNull(salvo.getAvaliadoEm());
        assertEquals("Nome Corrigido", json.readTree(salvo.getPayload()).get("responsavelNome").asText());
        assertEquals(1, preCadastros.count());

        // e o reenvio seguinte, ja pendente, volta a ser so idempotencia
        enviar(bearerAgente, payload(id, "Outro Nome"), "JA_RECEBIDO");
        assertEquals("Nome Corrigido",
            json.readTree(preCadastros.findById(id).orElseThrow().getPayload()).get("responsavelNome").asText());
    }

    @Test
    @DisplayName("o mesmo id devolvido enviado por OUTRA agente nao reabre nada")
    void reenvioDeOutraAgente() throws Exception {
        UUID id = UUID.randomUUID();
        enviar(bearerAgente, payload(id, "Nome Original"), "ACEITO");
        devolver(id, "Confira o nome.");

        enviar(bearerOutraAgente, payload(id, "Nome Alheio"), "JA_RECEBIDO");

        PreCadastro salvo = preCadastros.findById(id).orElseThrow();
        assertEquals(SituacaoPreCadastro.DEVOLVIDO, salvo.getSituacao());
        assertEquals("Nome Original", json.readTree(salvo.getPayload()).get("responsavelNome").asText());
    }

    // ------------------------------------------------------------ SYNC-02

    @Test
    @DisplayName("situacao: so os da propria agente, com motivo so quando devolvido")
    void situacaoDoAparelho() throws Exception {
        UUID pendente = UUID.randomUUID();
        UUID devolvido = UUID.randomUUID();
        UUID daOutra = UUID.randomUUID();
        enviar(bearerAgente, payload(pendente, "Responsável Um"), "ACEITO");
        enviar(bearerAgente, payload(devolvido, "Responsável Dois"), "ACEITO");
        enviar(bearerOutraAgente, payload(daOutra, "Responsável Três"), "ACEITO");
        devolver(devolvido, "Faltou a idade.");

        mvc.perform(get("/api/pre-cadastros/situacao")
                .param("ids", pendente + "," + devolvido + "," + daOutra + "," + UUID.randomUUID())
                .header("Authorization", bearerAgente))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(2)))
            .andExpect(jsonPath("$[?(@.id == '" + pendente + "')].situacao").value("PENDENTE"))
            .andExpect(jsonPath("$[?(@.id == '" + devolvido + "')].situacao").value("DEVOLVIDO"))
            .andExpect(jsonPath("$[?(@.id == '" + devolvido + "')].motivoDevolucao").value("Faltou a idade."))
            .andExpect(jsonPath("$[0].responsavelNome").doesNotExist());
    }

    @Test
    @DisplayName("situacao: 403 para admin, 401 sem token, 400 com ids demais")
    void situacaoAcesso() throws Exception {
        mvc.perform(get("/api/pre-cadastros/situacao").param("ids", UUID.randomUUID().toString())
                .header("Authorization", bearerAdmin))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/pre-cadastros/situacao").param("ids", UUID.randomUUID().toString()))
            .andExpect(status().isUnauthorized());

        String ids = IntStream.range(0, PreCadastroService.MAXIMO_IDS_POR_CONSULTA + 1)
            .mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.joining(","));
        mvc.perform(get("/api/pre-cadastros/situacao").param("ids", ids).header("Authorization", bearerAgente))
            .andExpect(status().isBadRequest());
    }
}
