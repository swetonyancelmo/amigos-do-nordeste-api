package br.org.amigosdonordeste.cadastro.config;

import br.org.amigosdonordeste.cadastro.agente.FiltroTokenAgente;
import br.org.amigosdonordeste.cadastro.auth.FiltroJwt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Dois tipos de credencial, dois papeis:
 *  - JWT da usuaria do painel  -> ROLE_ADMIN (FiltroJwt)
 *  - token do aparelho da agente -> ROLE_AGENTE (FiltroTokenAgente)
 *
 * Toda rota nova nasce ADMIN. A agente so entra onde estiver listada aqui
 * explicitamente — hoje, a rota de envio de pre-cadastro. Um token de aparelho
 * que vazasse nao abre a base.
 *
 * O controller dessa rota pode (e deve) repetir a regra com
 * {@code @PreAuthorize("hasRole('AGENTE')")}: @EnableMethodSecurity esta ligado.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SegurancaConfig {

    private final FiltroJwt filtroJwt;
    private final FiltroTokenAgente filtroTokenAgente;
    private final String origensPermitidas;

    public SegurancaConfig(FiltroJwt filtroJwt,
                           FiltroTokenAgente filtroTokenAgente,
                           @Value("${app.cors.origens}") String origensPermitidas) {
        this.filtroJwt = filtroJwt;
        this.filtroTokenAgente = filtroTokenAgente;
        this.origensPermitidas = origensPermitidas;
    }

    /**
     * argon2id e o padrao recomendado hoje para senha (ver ADR-0002).
     * Nunca guardamos a senha em texto, e nunca usamos hash rapido como MD5/SHA.
     * Parametros: salt 16, hash 32, 1 thread, 64 MiB de memoria, 3 iteracoes.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new Argon2PasswordEncoder(16, 32, 1, 65536, 3);
    }

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsSource()))
            // API sem sessao e sem formulario: CSRF via token nao se aplica, e o
            // cookie de renovacao e SameSite=Lax com caminho restrito.
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(rotas -> rotas
                // Trancado por padrao, aberto por excecao — nunca o contrario.
                // Toda rota nova ja nasce ADMIN sem voce fazer nada.
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/renovar",
                    "/api/auth/sair",
                    "/api/saude",
                    "/v3/api-docs/**",
                    "/api/metadados",
                    "/swagger-ui/**",
                    "/swagger-ui.html").permitAll()
                // A unica porta do aparelho da agente. So AGENTE: o administrador
                // nao envia pre-cadastro, ele aprova.
                .requestMatchers(HttpMethod.POST, "/api/pre-cadastros").hasRole("AGENTE")
                .anyRequest().hasRole("ADMIN"))
            .exceptionHandling(e -> e.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(filtroTokenAgente, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(origensPermitidas.split(","))
            .map(String::trim).filter(s -> !s.isEmpty()).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true); // necessario para o cookie de renovacao
        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", config);
        return fonte;
    }
}
