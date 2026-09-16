package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem (issue do POST /api/pre-cadastros):
 *  - o mesmo id enviado duas vezes cria UM registro e a segunda responde
 *    JA_RECEBIDO com 200 — nunca erro, nunca familia repetida;
 *  - pessoa sem nome passa quando cadastroIncompleto e true, e so nesse caso;
 *  - idadeEstimada e idadeEstimadaEm so entram juntos: um sem o outro e 400;
 *  - o payload guardado e o JSON como chegou, inclusive campo que o servidor
 *    ainda nao conhece;
 *  - o pre-cadastro fica amarrado a agente que enviou e a comunidade, quando
 *    o servidor a conhece.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreCadastroTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired AgenteRepositorio agentes;
    @Autowired ComunidadeRepositorio comunidades;
    @Autowired MunicipioRepositorio municipios;
    @Autowired TokenAgenteService tokens;

    private Agente agente;
    private String bearer;
    private Comunidade comunidade;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        agentes.deleteAll();
        comunidades.deleteAll();
        municipios.deleteAll();

        String token = tokens.gerar();
        agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setTokenHash(TokenAgenteService.hash(token));
        agente = agentes.save(agente);
        bearer = "Bearer " + token;

        Municipio municipio = municipios.save(Municipio.builder()
            .nome("Município de Teste").uf("PE").build());
        comunidade = comunidades.save(Comunidade.builder()
            .municipio(municipio).nome("Sítio de Teste").build());
    }

    /** Outras classes de teste apagam agentes; nao pode sobrar pre-cadastro apontando para eles. */
    @AfterEach
    void limpar() {
        preCadastros.deleteAll();
    }

    /** Payload valido e completo; os testes mexem so no que querem quebrar. */
    private String payload(UUID id, String pessoas) {
        return """
            {
              "id": "%s",
              "responsavelNome": "Responsável de Teste",
              "telefone": "87999990000",
              "comunidadeId": "%s",
              "comunidadeNome": "Sítio de Teste",
              "pontoReferencia": "Perto da escola",
              "criadoEm": "2026-09-12T09:12:00Z",
              "pessoas": [%s]
            }
            """.formatted(id, comunidade.getId(), pessoas);
    }

    private static final String PESSOA_COMPLETA = """
        { "id": "%s", "nome": "Criança de Teste", "cadastroIncompleto": false, "sexo": "FEMININO",
          "dataNascimento": null, "idadeEstimada": 4, "idadeEstimadaEm": "2026-09-12" }
        """.formatted(UUID.randomUUID());

    private MockHttpServletRequestBuilder enviar(String corpo) {
        return post("/api/pre-cadastros")
            .header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(corpo);
    }

    @Test
    @DisplayName("primeiro envio responde ACEITO e grava o pré-cadastro pendente")
    void primeiroEnvioEAceito() throws Exception {
        UUID id = UUID.randomUUID();

        mvc.perform(enviar(payload(id, PESSOA_COMPLETA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.situacao").value("ACEITO"));

        PreCadastro salvo = preCadastros.findById(id).orElseThrow();
        assertEquals(SituacaoPreCadastro.PENDENTE, salvo.getSituacao());
        assertEquals(agente.getId(), salvo.getAgente().getId());
        assertEquals(comunidade.getId(), salvo.getComunidade().getId());
        assertNotNull(salvo.getRecebidoEm());

        JsonNode guardado = json.readTree(salvo.getPayload());
        assertEquals("Responsável de Teste", guardado.get("responsavelNome").asText());
        assertEquals(1, guardado.get("pessoas").size());
    }

    @Test
    @DisplayName("o mesmo id enviado duas vezes cria um registro e responde JA_RECEBIDO na segunda")
    void mesmoIdDuasVezes() throws Exception {
        UUID id = UUID.randomUUID();
        String corpo = payload(id, PESSOA_COMPLETA);

        mvc.perform(enviar(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ACEITO"));

        mvc.perform(enviar(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id.toString()))
            .andExpect(jsonPath("$.situacao").value("JA_RECEBIDO"));

        // Terceira vez, como faria um app insistindo: continua sem duplicar.
        mvc.perform(enviar(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("JA_RECEBIDO"));

        assertEquals(1, preCadastros.count(), "o reenvio nao pode criar outro registro");
    }

    @Test
    @DisplayName("pessoa sem nome é aceita quando cadastroIncompleto é true")
    void pessoaSemNomeComCadastroIncompleto() throws Exception {
        String pessoa = """
            { "id": "%s", "nome": null, "cadastroIncompleto": true, "sexo": "FEMININO",
              "dataNascimento": null, "idadeEstimada": null, "idadeEstimadaEm": null }
            """.formatted(UUID.randomUUID());

        mvc.perform(enviar(payload(UUID.randomUUID(), pessoa)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ACEITO"));
    }

    @Test
    @DisplayName("pessoa sem nome é recusada quando o cadastro não está marcado como incompleto")
    void pessoaSemNomeSemMarcarIncompleto() throws Exception {
        String pessoa = """
            { "id": "%s", "nome": "  ", "cadastroIncompleto": false }
            """.formatted(UUID.randomUUID());

        mvc.perform(enviar(payload(UUID.randomUUID(), pessoa)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());
        assertEquals(0, preCadastros.count());
    }

    @Test
    @DisplayName("idadeEstimada sem idadeEstimadaEm é recusada com 400")
    void idadeEstimadaSemData() throws Exception {
        String pessoa = """
            { "id": "%s", "nome": "Criança de Teste", "cadastroIncompleto": false,
              "idadeEstimada": 4, "idadeEstimadaEm": null }
            """.formatted(UUID.randomUUID());

        mvc.perform(enviar(payload(UUID.randomUUID(), pessoa)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());
        assertEquals(0, preCadastros.count());
    }

    @Test
    @DisplayName("idadeEstimadaEm sem idadeEstimada também é recusada com 400")
    void idadeEstimadaEmSemIdade() throws Exception {
        String pessoa = """
            { "id": "%s", "nome": "Criança de Teste", "cadastroIncompleto": false,
              "idadeEstimada": null, "idadeEstimadaEm": "2026-09-12" }
            """.formatted(UUID.randomUUID());

        mvc.perform(enviar(payload(UUID.randomUUID(), pessoa)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());
        assertEquals(0, preCadastros.count());
    }

    @Test
    @DisplayName("campo que o servidor não conhece fica guardado no payload")
    void campoDesconhecidoFicaNoPayload() throws Exception {
        UUID id = UUID.randomUUID();
        String corpo = payload(id, PESSOA_COMPLETA)
            .replace("\"pontoReferencia\"", "\"campoNovoDoApp\": \"valor\", \"pontoReferencia\"");

        mvc.perform(enviar(corpo)).andExpect(status().isOk());

        JsonNode guardado = json.readTree(preCadastros.findById(id).orElseThrow().getPayload());
        assertEquals("valor", guardado.get("campoNovoDoApp").asText());
    }

    @Test
    @DisplayName("campo com formato errado é 400, não 500")
    void formatoErradoE400() throws Exception {
        String corpo = payload(UUID.randomUUID(), PESSOA_COMPLETA).replace("2026-09-12T09:12:00Z", "ontem");

        mvc.perform(enviar(corpo))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").isString());
    }

    @Test
    @DisplayName("sem id não há como reconhecer o reenvio: 400")
    void semIdERecusado() throws Exception {
        String corpo = payload(UUID.randomUUID(), PESSOA_COMPLETA).replaceFirst("\"id\": \"[^\"]+\",", "");

        mvc.perform(enviar(corpo)).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("comunidade que o servidor não conhece não impede o envio")
    void comunidadeDesconhecidaEntraSemVinculo() throws Exception {
        UUID id = UUID.randomUUID();
        String corpo = payload(id, PESSOA_COMPLETA)
            .replace(comunidade.getId().toString(), UUID.randomUUID().toString());

        mvc.perform(enviar(corpo))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.situacao").value("ACEITO"));

        assertNull(preCadastros.findById(id).orElseThrow().getComunidade());
    }
}
