package br.org.amigosdonordeste.cadastro.config;

import br.org.amigosdonordeste.cadastro.agente.Agente;
import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O que estes testes protegem (issue #31):
 *  - um token de aparelho da agente nao abre nenhuma rota de administracao;
 *  - um token de administrador nao envia pre-cadastro;
 *  - um token de aparelho desconhecido ou de agente desativada nao autentica.
 *
 * A rota POST /api/pre-cadastros ainda nao tem controller (issue #33). A regra
 * de acesso roda antes do roteamento, entao aqui o que se confere e: com o
 * papel errado e 403; com o papel certo NAO e 401 nem 403.
 *
 * Dados ficticios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SegurancaPapeisTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired AgenteRepositorio agentes;
    @Autowired JwtService jwt;
    @Autowired TokenAgenteService tokens;

    private String bearerAdmin;
    private String bearerAgente;
    private String bearerAgenteInativa;

    @BeforeEach
    void preparar() {
        agentes.deleteAll();
        usuarios.deleteAll();

        Usuario admin = new Usuario();
        admin.setNome("Admin de Teste");
        admin.setEmail("admin@teste.local");
        admin.setSenhaHash("hash-qualquer");
        admin.setPapel(Papel.ADMIN);
        admin = usuarios.save(admin);
        bearerAdmin = "Bearer " + jwt.gerarAcesso(admin.getId(), admin.getEmail(), admin.getPapel());

        bearerAgente = "Bearer " + salvarAgente("Agente de Teste", true);
        bearerAgenteInativa = "Bearer " + salvarAgente("Agente Desativada", false);
    }

    private String salvarAgente(String nome, boolean ativa) {
        String token = tokens.gerar();
        Agente agente = new Agente();
        agente.setNome(nome);
        agente.setTokenHash(TokenAgenteService.hash(token));
        agente.setAtivo(ativa);
        agentes.save(agente);
        return token;
    }

    @Test
    @DisplayName("token de agente não abre nenhuma rota de administração")
    void agenteNaoAbreAdministracao() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/comunidades").header("Authorization", bearerAgente))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").header("Authorization", bearerAgente)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/trocar-senha").header("Authorization", bearerAgente)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token de administrador não envia pré-cadastro")
    void adminNaoEnviaPreCadastro() throws Exception {
        mvc.perform(post("/api/pre-cadastros").header("Authorization", bearerAdmin)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token de agente passa pela autorização da rota de envio")
    void agentePassaNaRotaDeEnvio() throws Exception {
        int status = mvc.perform(post("/api/pre-cadastros").header("Authorization", bearerAgente)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andReturn().getResponse().getStatus();
        assertFalse(status == 401 || status == 403,
            "a agente deveria passar pela autorização, mas recebeu " + status);
    }

    @Test
    @DisplayName("token de administrador abre o painel")
    void adminAbrePainel() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", bearerAdmin))
            .andExpect(status().isOk());
    }

    @Test
    @DisplayName("sem token, token desconhecido ou agente desativada: 401")
    void tokenInvalidoNaoAutentica() throws Exception {
        mvc.perform(post("/api/pre-cadastros")
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pre-cadastros").header("Authorization", "Bearer " + tokens.gerar())
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/pre-cadastros").header("Authorization", bearerAgenteInativa)
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/usuarios").header("Authorization", bearerAgenteInativa))
            .andExpect(status().isUnauthorized());
    }
}
