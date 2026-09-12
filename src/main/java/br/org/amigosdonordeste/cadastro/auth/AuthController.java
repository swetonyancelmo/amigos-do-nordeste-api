package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.auth.dto.LoginRequisicao;
import br.org.amigosdonordeste.cadastro.auth.dto.LoginResposta;
import br.org.amigosdonordeste.cadastro.auth.dto.TrocarSenhaRequisicao;
import br.org.amigosdonordeste.cadastro.comum.dto.ErroResposta;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @Operation(summary = "Entrar no sistema",
        description = "Autentica o usuário com e-mail e senha. Em caso de sucesso, define o cookie "
            + "httpOnly 'and_refresh' com o refresh token e retorna o access token no corpo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login realizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
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

    @Operation(summary = "Renovar token de acesso",
        description = "Gera um novo access token a partir do refresh token enviado no cookie httpOnly 'and_refresh'.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado com sucesso"),
        @ApiResponse(responseCode = "401", description = "Refresh token ausente, inválido ou expirado",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
    @PostMapping("/renovar")
    public Map<String, String> renovar(
        @CookieValue(name = COOKIE_RENOVACAO, required = false) String tokenRenovacao,
        HttpServletResponse resposta) {
        AuthService.Tokens tokens = auth.renovar(tokenRenovacao);
        gravarCookie(resposta, tokens.renovacao());
        return Map.of("accessToken", tokens.acesso());
    }

    @Operation(summary = "Sair do sistema",
        description = "Limpa o cookie de refresh token (and_refresh). Operação idempotente mesmo sem sessão ativa.")
    @ApiResponse(responseCode = "200", description = "Logout realizado com sucesso")
    @PostMapping("/sair")
    public ResponseEntity<Map<String, Boolean>> sair() {
        ResponseCookie limpo = ResponseCookie.from(COOKIE_RENOVACAO, "")
            .httpOnly(true).secure(cookieSeguro).sameSite("Lax").path("/api/auth").maxAge(0).build();
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, limpo.toString())
            .body(Map.of("ok", true));
    }

    @Operation(summary = "Trocar senha do usuário autenticado",
        description = "Requer token Bearer válido. Valida a senha atual antes de aplicar a nova senha.")
    @SecurityRequirement(name = "bearer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Senha alterada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Senha atual incorreta ou dados inválidos",
            content = @Content(schema = @Schema(implementation = ErroResposta.class))),
        @ApiResponse(responseCode = "401", description = "Não autenticado",
            content = @Content(schema = @Schema(implementation = ErroResposta.class)))
    })
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
