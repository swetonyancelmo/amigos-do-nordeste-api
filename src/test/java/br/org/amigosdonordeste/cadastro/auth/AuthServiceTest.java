package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.usuario.Papel;
import br.org.amigosdonordeste.cadastro.usuario.Usuario;
import br.org.amigosdonordeste.cadastro.usuario.UsuarioRepositorio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.Optional;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock
  private UsuarioRepositorio usuarios;

  @Mock
  private PasswordEncoder codificador;

  @Mock
  private JwtService jwt;

  @InjectMocks
  private AuthService authService;

  @Test
  @DisplayName("Deve autenticar com sucesso quando as credenciais forem válidas")
  void deveAutenticarComSucesso() {
    UUID id = UUID.randomUUID();
    Usuario usuario = new Usuario();
    usuario.setId(id);
    usuario.setEmail("maria.silva@amigosdonordeste.org.br");
    usuario.setSenhaHash("hash_valido");
    usuario.setPapel(Papel.ADMIN);

    given(usuarios.findByEmailIgnoreCaseAndAtivoTrue(anyString())).willReturn(Optional.of(usuario));
    given(codificador.matches("SenhaForte123", "hash_valido")).willReturn(true);
    given(jwt.gerarAcesso(any(), any(), any())).willReturn("access_token");
    given(jwt.gerarRenovacao(any(), any())).willReturn("refresh_token");

    AuthService.Tokens tokens = authService.entrar("maria.silva@amigosdonordeste.org.br", "SenhaForte123");

    assertNotNull(tokens);
    assertEquals("access_token", tokens.acesso());
    assertEquals("refresh_token", tokens.renovacao());
    verify(usuarios).save(usuario);
  }

  @Test
  @DisplayName("Deve lançar CredenciaisInvalidasException quando o e-mail não existir")
  void deveLancarExcecaoQuandoEmailNaoExistir() {
    given(usuarios.findByEmailIgnoreCaseAndAtivoTrue(anyString())).willReturn(Optional.empty());

    assertThrows(CredenciaisInvalidasException.class, () ->
      authService.entrar("inexistente@amigosdonordeste.org.br", "SenhaForte123")
    );

    // Garante que o encoder rodou para mitigar timing attack
    verify(codificador).encode("descarte-tempo-constante");
  }
}
