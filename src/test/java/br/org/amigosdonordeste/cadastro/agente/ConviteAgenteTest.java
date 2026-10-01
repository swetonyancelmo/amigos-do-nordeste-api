package br.org.amigosdonordeste.cadastro.agente;

import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Painel cadastra a agente e gera o convite (auditoria de integracao, API-01).
 *
 * O que se protege:
 *  - so ADMIN cria, lista e reemite; a propria agente nao;
 *  - o codigo gerado ativa o aparelho;
 *  - reemitir o convite derruba o token do aparelho antigo, na mesma agente.
 *
 * Dados ficticios.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConviteAgenteTest {

    @Autowired MockMvc mvc;
    @Autowired AgenteRepositorio agentes;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;
    @Autowired ObjectMapper json;

    private String bearerAdmin;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        agentes.deleteAll();
        usuarios.deleteAll();

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearerAdmin = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());
    }

    private JsonNode criar(String nome) throws Exception {
        String corpo = mvc.perform(post("/api/agentes").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"" + nome + "\"}"))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo);
    }

    private String ativar(String codigo, String ip) throws Exception {
        String corpo = mvc.perform(post("/api/agentes/ativar")
                .with(req -> { req.setRemoteAddr(ip); return req; })
                .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"" + codigo + "\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return json.readTree(corpo).get("token").asText();
    }

    @Test
    @DisplayName("cria a agente com um codigo de seis digitos que ativa o aparelho")
    void criaEAtiva() throws Exception {
        JsonNode agente = criar("Agente de Teste");
        String codigo = agente.get("codigoConvite").asText();

        assertEquals(6, codigo.length());
        assertEquals("Agente de Teste", agente.get("nome").asText());

        ativar(codigo, "10.1.0.1");

        mvc.perform(get("/api/agentes").header(HttpHeaders.AUTHORIZATION, bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].codigoConvite").doesNotExist())
            .andExpect(jsonPath("$[0].ativadoEm").isNotEmpty())
            .andExpect(jsonPath("$[0].tokenHash").doesNotExist());
    }

    @Test
    @DisplayName("nome em branco e 400")
    void nomeEmBranco() throws Exception {
        mvc.perform(post("/api/agentes").header(HttpHeaders.AUTHORIZATION, bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\" \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("sem token e 401; com token de agente e 403")
    void soAdmin() throws Exception {
        mvc.perform(post("/api/agentes").contentType(MediaType.APPLICATION_JSON).content("{\"nome\":\"X\"}"))
            .andExpect(status().isUnauthorized());

        String token = ativar(criar("Agente de Teste").get("codigoConvite").asText(), "10.1.0.2");
        mvc.perform(get("/api/agentes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("novo convite derruba o aparelho antigo e ativa o novo na mesma agente")
    void novoConvite() throws Exception {
        JsonNode agente = criar("Agente de Teste");
        String id = agente.get("id").asText();
        String tokenAntigo = ativar(agente.get("codigoConvite").asText(), "10.1.0.3");

        String corpo = mvc.perform(post("/api/agentes/" + id + "/novo-convite")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andReturn().getResponse().getContentAsString();
        String codigoNovo = json.readTree(corpo).get("codigoConvite").asText();

        // o token antigo nao autentica mais
        mvc.perform(get("/api/comunidades/opcoes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenAntigo))
            .andExpect(status().isUnauthorized());

        String tokenNovo = ativar(codigoNovo, "10.1.0.3");
        assertNotEquals(tokenAntigo, tokenNovo);
        mvc.perform(get("/api/comunidades/opcoes").header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenNovo))
            .andExpect(status().isOk());

        assertEquals(1, agentes.count(), "reemitir nao cria outra agente");
    }

    @Test
    @DisplayName("novo convite para agente inexistente e 404")
    void novoConviteInexistente() throws Exception {
        mvc.perform(post("/api/agentes/" + UUID.randomUUID() + "/novo-convite")
                .header(HttpHeaders.AUTHORIZATION, bearerAdmin))
            .andExpect(status().isNotFound());
    }
}
