package br.org.amigosdonordeste.cadastro.agente;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem:
 *  - o mesmo codigo de convite nao funciona duas vezes;
 *  - o banco nao guarda o token em claro, so o hash;
 *  - codigo inexistente e codigo ja usado dao a MESMA resposta;
 *  - tentativas repetidas do mesmo IP sao recusadas;
 *  - o token entregue de fato autentica a agente.
 *
 * O limite e por IP e vive na JVM, entao cada teste usa um IP proprio para
 * um nao esbarrar no contador do outro. Dados ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AtivacaoAgenteTest {

    @Autowired MockMvc mvc;
    @Autowired AgenteRepositorio agentes;
    @Autowired ObjectMapper json;

    @BeforeEach
    void preparar() {
        agentes.deleteAll();
    }

    private Agente salvarAgente(String codigo) {
        Agente agente = new Agente();
        agente.setNome("Agente de Teste");
        agente.setCodigoConvite(codigo);
        return agentes.save(agente);
    }

    private MockHttpServletRequestBuilder ativar(String codigo, String ip) {
        return post("/api/agentes/ativar")
            .with(req -> { req.setRemoteAddr(ip); return req; })
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"codigo\":\"" + codigo + "\"}");
    }

    @Test
    @DisplayName("troca o código pelo token e devolve o nome da agente")
    void ativaComCodigoValido() throws Exception {
        salvarAgente("111111");

        mvc.perform(ativar("111111", "10.0.0.1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nomeAgente").value("Agente de Teste"))
            .andExpect(jsonPath("$.token").isString());
    }

    @Test
    @DisplayName("o mesmo código não funciona duas vezes")
    void codigoEDeUsoUnico() throws Exception {
        Agente agente = salvarAgente("222222");

        mvc.perform(ativar("222222", "10.0.0.2")).andExpect(status().isOk());
        mvc.perform(ativar("222222", "10.0.0.2")).andExpect(status().isUnauthorized());

        assertNull(agentes.findById(agente.getId()).orElseThrow().getCodigoConvite(),
            "o codigo deveria virar null depois de trocado");
    }

    @Test
    @DisplayName("o banco guarda só o hash do token, nunca o token em claro")
    void bancoNaoGuardaTokenEmClaro() throws Exception {
        Agente agente = salvarAgente("333333");

        MvcResult resultado = mvc.perform(ativar("333333", "10.0.0.3"))
            .andExpect(status().isOk()).andReturn();
        String token = json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();

        Agente salvo = agentes.findById(agente.getId()).orElseThrow();
        assertNotEquals(token, salvo.getTokenHash());
        assertFalse(salvo.getTokenHash().contains(token));
        assertEquals(TokenAgenteService.hash(token), salvo.getTokenHash());
        assertNotNull(salvo.getAtivadoEm());
    }

    @Test
    @DisplayName("código inexistente e código já usado devolvem a mesma resposta")
    void codigoInexistenteEUsadoSaoIndistinguiveis() throws Exception {
        salvarAgente("444444");
        mvc.perform(ativar("444444", "10.0.0.4")).andExpect(status().isOk());

        MvcResult usado = mvc.perform(ativar("444444", "10.0.0.4")).andReturn();
        MvcResult inexistente = mvc.perform(ativar("999999", "10.0.0.4")).andReturn();

        assertEquals(401, usado.getResponse().getStatus());
        assertEquals(401, inexistente.getResponse().getStatus());
        JsonNode corpoUsado = json.readTree(usado.getResponse().getContentAsString());
        JsonNode corpoInexistente = json.readTree(inexistente.getResponse().getContentAsString());
        assertEquals(corpoUsado.get("message"), corpoInexistente.get("message"));
    }

    @Test
    @DisplayName("agente desativada não ativa, mesmo com código certo")
    void agenteDesativadaNaoAtiva() throws Exception {
        Agente agente = salvarAgente("555555");
        agente.setAtivo(false);
        agentes.save(agente);

        mvc.perform(ativar("555555", "10.0.0.5")).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("tentativas repetidas do mesmo IP são recusadas com 429")
    void limitaTentativasPorIp() throws Exception {
        salvarAgente("666666");

        // 5 por minuto (application.yml). Erra cinco vezes...
        for (int i = 0; i < 5; i++) {
            mvc.perform(ativar("000000", "10.0.0.6")).andExpect(status().isUnauthorized());
        }
        // ...e a sexta e barrada antes mesmo de olhar o codigo, ainda que esteja certo.
        mvc.perform(ativar("666666", "10.0.0.6")).andExpect(status().isTooManyRequests());

        // Outro IP nao e afetado.
        mvc.perform(ativar("666666", "10.0.0.7")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("código fora do formato é rejeitado antes de contar como tentativa")
    void codigoForaDoFormato() throws Exception {
        mvc.perform(ativar("abc", "10.0.0.8")).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("o token entregue autentica a agente na rota de envio")
    void tokenEntregueAutentica() throws Exception {
        salvarAgente("777777");
        MvcResult resultado = mvc.perform(ativar("777777", "10.0.0.9")).andReturn();
        String token = json.readTree(resultado.getResponse().getContentAsString()).get("token").asText();

        int status = mvc.perform(post("/api/pre-cadastros")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus();
        assertFalse(status == 401 || status == 403,
            "o token recem-entregue deveria autenticar, mas recebeu " + status);
    }
}
