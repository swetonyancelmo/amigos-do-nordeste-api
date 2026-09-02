package br.org.amigosdonordeste.cadastro.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le o token do cabecalho Authorization e coloca a usuaria no contexto.
 *
 * Um token de renovacao NAO abre rota protegida — ele so serve para renovar.
 * Por isso o tipo e conferido aqui.
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
            && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Claims claims = jwt.ler(cabecalho.substring(7));
                if (JwtService.TIPO_ACESSO.equals(claims.get("tipo", String.class))) {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of());
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
