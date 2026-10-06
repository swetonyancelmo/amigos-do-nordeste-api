package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * O painel guarda token e usuário só em memória: depois de recarregar a
 * página, a renovação é a única chamada que diz quem está logado. Por isso
 * ela devolve o mesmo resumo do login.
 *
 * Dados fictícios — nenhum nome real entra em teste.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RenovarSessaoTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired JwtService jwt;

    @Test
    @DisplayName("renovar devolve o token novo e o resumo do usuário")
    void renovarDevolveUsuario() throws Exception {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuária de Teste");
        usuario.setEmail("renovar-" + UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash("hash-qualquer");
        usuario.setPapel(Papel.ADMIN);
        usuario = usuarios.save(usuario);

        String renovacao = jwt.gerarRenovacao(usuario.getId(), usuario.getEmail());

        mvc.perform(post("/api/auth/renovar").cookie(new Cookie("and_refresh", renovacao)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.usuario.id").value(usuario.getId().toString()))
            .andExpect(jsonPath("$.usuario.nome").value("Usuária de Teste"))
            .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()));
    }

    @Test
    @DisplayName("renovar sem cookie continua respondendo 401")
    void renovarSemCookie() throws Exception {
        mvc.perform(post("/api/auth/renovar")).andExpect(status().isUnauthorized());
    }
}
