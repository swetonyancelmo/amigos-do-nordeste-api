package br.org.amigosdonordeste.cadastro.agente;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Le o token do aparelho (Authorization: Bearer agente_...) e coloca a agente
 * no contexto com ROLE_AGENTE — e so isso.
 *
 * O que essa authority abre esta em SegurancaConfig: a rota de envio de
 * pre-cadastro. Todo o resto exige ROLE_ADMIN, entao um token de aparelho que
 * vazasse nao lista familia, nao ve relatorio e nao abre o painel.
 *
 * O principal e o id do agente (string), no mesmo formato que o FiltroJwt usa
 * para o usuario: nos controllers, {@code @AuthenticationPrincipal String agenteId}.
 */
@Component
public class FiltroTokenAgente extends OncePerRequestFilter {

    public static final String PAPEL = "AGENTE";

    private final AgenteRepositorio agentes;

    public FiltroTokenAgente(AgenteRepositorio agentes) {
        this.agentes = agentes;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest requisicao,
                                    @NonNull HttpServletResponse resposta,
                                    @NonNull FilterChain cadeia)
        throws ServletException, IOException {

        String cabecalho = requisicao.getHeader("Authorization");

        if (cabecalho != null && cabecalho.startsWith("Bearer ")
            && TokenAgenteService.eTokenDeAgente(cabecalho.substring(7))
            && SecurityContextHolder.getContext().getAuthentication() == null) {

            // Token desconhecido ou agente desativado: segue sem autenticar e a
            // requisicao termina em 401 no ponto certo.
            agentes.findByTokenHashAndAtivoTrue(TokenAgenteService.hash(cabecalho.substring(7)))
                .ifPresent(agente -> {
                    var autenticacao = new UsernamePasswordAuthenticationToken(
                        agente.getId().toString(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + PAPEL)));
                    autenticacao.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(requisicao));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                });
        }

        cadeia.doFilter(requisicao, resposta);
    }
}
