package br.org.amigosdonordeste.cadastro.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    public static final String TIPO_ACESSO = "access";
    public static final String TIPO_RENOVACAO = "refresh";

    private final SecretKey chave;
    private final Duration validadeAcesso;
    private final Duration validadeRenovacao;

    public JwtService(
        @Value("${app.jwt.segredo}") String segredo,
        @Value("${app.jwt.validade-acesso}") Duration validadeAcesso,
        @Value("${app.jwt.validade-renovacao}") Duration validadeRenovacao
    ) {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                "APP_JWT_SEGREDO precisa ter ao menos 32 bytes. Gere com: "
                + "openssl rand -base64 48");
        }
        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeAcesso = validadeAcesso;
        this.validadeRenovacao = validadeRenovacao;
    }

    public String gerarAcesso(UUID usuarioId, String email) {
        return gerar(usuarioId, email, TIPO_ACESSO, validadeAcesso);
    }

    public String gerarRenovacao(UUID usuarioId, String email) {
        return gerar(usuarioId, email, TIPO_RENOVACAO, validadeRenovacao);
    }

    public Duration getValidadeRenovacao() {
        return validadeRenovacao;
    }

    private String gerar(UUID usuarioId, String email, String tipo, Duration validade) {
        long agora = System.currentTimeMillis();
        return Jwts.builder()
            .subject(usuarioId.toString())
            .claim("email", email)
            .claim("tipo", tipo)
            .issuedAt(new Date(agora))
            .expiration(new Date(agora + validade.toMillis()))
            .signWith(chave)
            .compact();
    }

    /** Lanca JwtException se o token for invalido ou estiver expirado. */
    public Claims ler(String token) {
        return Jwts.parser().verifyWith(chave).build().parseSignedClaims(token).getPayload();
    }
}
