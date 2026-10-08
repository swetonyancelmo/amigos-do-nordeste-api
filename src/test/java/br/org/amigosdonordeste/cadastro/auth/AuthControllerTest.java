package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Dados fictícios — nenhum nome real entra em teste. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepositorio usuarios;
    @Autowired PasswordEncoder codificador;

    private Usuario criarUsuario(String senha) {
        Usuario usuario = new Usuario();
        usuario.setNome("Usuária de Teste");
        usuario.setEmail("login-" + UUID.randomUUID() + "@teste.local");
        usuario.setSenhaHash(codificador.encode(senha));
        usuario.setPapel(Papel.ADMIN);
        return usuarios.save(usuario);
    }

    private static String corpo(String email, String senha) {
        return "{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}";
    }

    @Test
    @DisplayName("login devolve o token e grava o refresh em cookie httpOnly")
    void loginGravaCookie() throws Exception {
        Usuario usuario = criarUsuario("SenhaDeTeste123");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(usuario.getEmail(), "SenhaDeTeste123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
            .andExpect(header().string("Set-Cookie", containsString("and_refresh=")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @DisplayName("login com senha errada responde 401 e não grava cookie")
    void loginSenhaErrada() throws Exception {
        Usuario usuario = criarUsuario("SenhaDeTeste123");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corpo(usuario.getEmail(), "outra-senha")))
            .andExpect(status().isUnauthorized())
            .andExpect(header().doesNotExist("Set-Cookie"));
    }
}
