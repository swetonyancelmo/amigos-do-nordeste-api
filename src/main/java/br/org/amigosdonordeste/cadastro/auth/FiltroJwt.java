package br.org.amigosdonordeste.cadastro.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import br.org.amigosdonordeste.cadastro.agente.TokenAgenteService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le o JWT do cabecalho Authorization e coloca a usuaria no contexto, com a
 * authority ROLE_&lt;papel&gt; que veio dentro do token.
 *
 * Um token de renovacao NAO abre rota protegida — ele so serve para renovar.
 * Por isso o tipo e conferido aqui. Um token de acesso sem papel (emitido
 * antes da V10) tambem nao abre nada: a pessoa entra de novo e recebe um novo.
 *
 * Token de aparelho (prefixo agente_) nao e JWT — quem cuida dele e o
 * FiltroTokenAgente.
 */
@Component
public class FiltroJwt extends OncePerRequestFilter {

    private final JwtService jwt;

    public FiltroJwt(JwtService jwt) {
        this.jwt = jwt;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain cadeia)
        throws ServletException, IOException {

        String cabecalho = requisicao.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith("Bearer ")
            && !TokenAgenteService.eTokenDeAgente(cabecalho.substring(7))
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwt.ler(cabecalho.substring(7));
                String papel = claims.get("papel", String.class);
                if (JwtService.TIPO_ACESSO.equals(claims.get("tipo", String.class)) && papel != null) {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + papel)));
                    autenticacao.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(requisicao));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            } catch (JwtException e) {
                // Token invalido ou expirado: segue sem autenticacao, e a
                // requisicao termina em 401 no ponto certo.
                SecurityContextHolder.clearContext();
            }
        }

        cadeia.doFilter(requisicao, resposta);
    }
}
