package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.agente.AgenteRepositorio;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;


@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private AgenteRepositorio agenteRepositorio;

  @MockitoBean
  private AuthService authService;

  // outros @MockitoBean que o filtro/config exigirem

  @Test
  void deveFazerLoginEGravarCookie() throws Exception {
    // when(authService.login(...)).thenReturn(...);
    // mockMvc.perform(post("/auth/login")...)
  }
}
