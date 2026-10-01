package br.org.amigosdonordeste.cadastro.config;

import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import br.org.amigosdonordeste.cadastro.auth.JwtService;
import br.org.amigosdonordeste.cadastro.precadastro.PreCadastroRepositorio;
import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Auditoria de integracao, API-02: erro que passa por sendError (404, 405,
 * 403 de @PreAuthorize) e redespachado para /error. Se esse despacho exigir
 * ADMIN, ele chega sem autenticacao (a sessao e STATELESS) e a resposta vira
 * 401 — o web acha que a sessao caiu e o app acha que perdeu o acesso.
 *
 * MockMvc nao faz o despacho de erro, por isso este teste sobe o servidor de
 * verdade numa porta aleatoria.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class DespachoDeErroTest {

    @Autowired TestRestTemplate http;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired AgenteRepositorio agentes;
    @Autowired PreCadastroRepositorio preCadastros;
    @Autowired JwtService jwt;

    private HttpHeaders admin;

    @BeforeEach
    void preparar() {
        preCadastros.deleteAll();
        agentes.deleteAll();
        usuarios.deleteAll();

        Usuario u = new Usuario();
        u.setNome("Admin de Teste");
        u.setEmail("admin@teste.local");
        u.setSenhaHash("hash-qualquer");
        u.setPapel(Papel.ADMIN);
        u = usuarios.save(u);

        admin = new HttpHeaders();
        admin.setBearerAuth(jwt.gerarAcesso(u.getId(), u.getEmail(), u.getPapel()));
    }

    @Test
    @DisplayName("rota inexistente com token de admin responde 404, nao 401")
    void rotaInexistente() {
        ResponseEntity<String> r = http.exchange("/api/rota-que-nao-existe", HttpMethod.GET,
            new HttpEntity<>(admin), String.class);
        assertEquals(404, r.getStatusCode().value());
    }

    @Test
    @DisplayName("metodo nao suportado com token de admin responde 405, nao 401")
    void metodoNaoSuportado() {
        ResponseEntity<String> r = http.exchange("/api/municipios", HttpMethod.DELETE,
            new HttpEntity<>(admin), String.class);
        assertEquals(405, r.getStatusCode().value());
    }

    @Test
    @DisplayName("sem token continua 401")
    void semToken() {
        ResponseEntity<String> r = http.getForEntity("/api/municipios", String.class);
        assertEquals(401, r.getStatusCode().value());
    }
}
