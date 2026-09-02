package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.auth.dto.LoginRequisicao;
import br.org.amigosdonordeste.cadastro.auth.dto.LoginResposta;
import br.org.amigosdonordeste.cadastro.auth.dto.TrocarSenhaRequisicao;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * NAO EXISTE ROTA DE CADASTRO AQUI, E ISSO E PROPOSITAL.
 *
 * O sistema tem uma usuaria, por decisao da associacao. Uma rota publica de
 * registro seria um buraco de seguranca sem nenhum beneficio: a conta nasce do
 * comando de criacao de usuario, rodado uma vez na instalacao.
 * Ver docs/decisoes/ADR-0002.
 */
@Tag(name = "auth")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String COOKIE_RENOVACAO = "and_refresh";

    private final AuthService auth;
    private final boolean cookieSeguro;

    public AuthController(AuthService auth,
                          @org.springframework.beans.factory.annotation.Value("${app.cookie-seguro:false}")
                          boolean cookieSeguro) {
        this.auth = auth;
        this.cookieSeguro = cookieSeguro;
    }

    @Operation(summary = "Entrar no sistema")
    @PostMapping("/login")
    public LoginResposta entrar(@Valid @RequestBody LoginRequisicao requisicao,
                                HttpServletResponse resposta) {
        AuthService.Tokens tokens = auth.entrar(requisicao.email(), requisicao.senha());
        gravarCookie(resposta, tokens.renovacao());

        // O access token vai no corpo e vive so em memoria no frontend.
        // O de renovacao vai em cookie httpOnly — nenhum dos dois em localStorage.
        return new LoginResposta(
            new LoginResposta.UsuarioResumo(
                tokens.usuario().getId(), tokens.usuario().getNome(), tokens.usuario().getEmail()),
            tokens.acesso());
    }

    @PostMapping("/renovar")
    public Map<String, String> renovar(
        @CookieValue(name = COOKIE_RENOVACAO, required = false) String tokenRenovacao,
        HttpServletResponse resposta) {
        AuthService.Tokens tokens = auth.renovar(tokenRenovacao);
        gravarCookie(resposta, tokens.renovacao());
        return Map.of("accessToken", tokens.acesso());
    }

    @PostMapping("/sair")
    public ResponseEntity<Map<String, Boolean>> sair() {
        ResponseCookie limpo = ResponseCookie.from(COOKIE_RENOVACAO, "")
            .httpOnly(true).secure(cookieSeguro).sameSite("Lax").path("/api/auth").maxAge(0).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, limpo.toString())
            .body(Map.of("ok", true));
    }

    @PostMapping("/trocar-senha")
    public Map<String, Boolean> trocarSenha(@AuthenticationPrincipal String usuarioId,
                                            @Valid @RequestBody TrocarSenhaRequisicao requisicao) {
        auth.trocarSenha(UUID.fromString(usuarioId), requisicao.senhaAtual(), requisicao.senhaNova());
        return Map.of("ok", true);
    }

    private void gravarCookie(HttpServletResponse resposta, String tokenRenovacao) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_RENOVACAO, tokenRenovacao)
            .httpOnly(true)          // o JavaScript da pagina nao le — reduz o estrago de um XSS
            .secure(cookieSeguro)
            .sameSite("Lax")
            .path("/api/auth")
            .maxAge(java.time.Duration.ofDays(7))
            .build();
        resposta.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
