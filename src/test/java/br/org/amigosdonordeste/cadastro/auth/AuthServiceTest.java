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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import io.jsonwebtoken.JwtException;

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
    usuario.setEmail("usuaria@teste.local");
    usuario.setSenhaHash("hash_valido");
    usuario.setPapel(Papel.ADMIN);

    given(usuarios.findByEmailIgnoreCaseAndAtivoTrue(anyString())).willReturn(Optional.of(usuario));
    given(codificador.matches("SenhaForte123", "hash_valido")).willReturn(true);
    given(jwt.gerarAcesso(any(), any(), any())).willReturn("access_token");
    given(jwt.gerarRenovacao(any(), any())).willReturn("refresh_token");

    AuthService.Tokens tokens = authService.entrar("usuaria@teste.local", "SenhaForte123");

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
      authService.entrar("inexistente@teste.local", "SenhaForte123")
    );

    // Garante que o encoder rodou para mitigar timing attack
    verify(codificador).encode("descarte-tempo-constante");
  }

  @Test
  @DisplayName("Deve lançar CredenciaisInvalidasException e não salvar quando a senha estiver errada")
  void deveLancarExcecaoQuandoSenhaErrada() {
    Usuario usuario = new Usuario();
    usuario.setEmail("usuaria@teste.local");
    usuario.setSenhaHash("hash_valido");

    given(usuarios.findByEmailIgnoreCaseAndAtivoTrue(anyString())).willReturn(Optional.of(usuario));
    given(codificador.matches("senha-errada", "hash_valido")).willReturn(false);

    assertThrows(CredenciaisInvalidasException.class, () ->
      authService.entrar("usuaria@teste.local", "senha-errada")
    );
    verify(usuarios, never()).save(any());
  }

  @Test
  @DisplayName("renovar rejeita token vazio")
  void renovarRejeitaTokenVazio() {
    assertThrows(CredenciaisInvalidasException.class, () -> authService.renovar(" "));
  }

  @Test
  @DisplayName("renovar rejeita token ilegível")
  void renovarRejeitaTokenInvalido() {
    given(jwt.ler("lixo")).willThrow(new JwtException("inválido"));

    assertThrows(CredenciaisInvalidasException.class, () -> authService.renovar("lixo"));
  }
}
