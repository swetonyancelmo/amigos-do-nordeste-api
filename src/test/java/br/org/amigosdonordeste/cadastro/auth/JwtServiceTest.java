package br.org.amigosdonordeste.cadastro.auth;

import br.org.amigosdonordeste.cadastro.usuario.Papel;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O que estes testes protegem:
 *  - um token de renovação não pode abrir rota protegida;
 *  - um token assinado com outro segredo não pode ser aceito;
 *  - um segredo fraco derruba a aplicação na subida, em vez de deixar o sistema
 *    rodando com assinatura fácil de forjar.
 */
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-trinta-e-dois-bytes-aqui";

    private JwtService servico(String segredo) {
        return new JwtService(segredo, Duration.ofMinutes(15), Duration.ofDays(7));
    }

    @Test
    @DisplayName("o token de acesso carrega o id da usuária, o tipo access e o papel")
    void tokenDeAcesso() {
        UUID id = UUID.randomUUID();
        Claims claims = servico(SEGREDO).ler(servico(SEGREDO).gerarAcesso(id, "a@b.com", Papel.ADMIN));
        assertEquals(id.toString(), claims.getSubject());
        assertEquals("access", claims.get("tipo", String.class));
        assertEquals("a@b.com", claims.get("email", String.class));
        assertEquals("ADMIN", claims.get("papel", String.class));
    }

    @Test
    @DisplayName("o token de renovação é marcado como refresh e não como access")
    void tokenDeRenovacaoNaoEDeAcesso() {
        JwtService jwt = servico(SEGREDO);
        Claims claims = jwt.ler(jwt.gerarRenovacao(UUID.randomUUID(), "a@b.com"));
        assertEquals("refresh", claims.get("tipo", String.class));
        assertNotEquals("access", claims.get("tipo", String.class));
        assertNull(claims.get("papel", String.class), "o token de renovação não carrega papel");
    }

    @Test
    @DisplayName("token assinado com outro segredo é recusado")
    void assinaturaDeOutroSegredo() {
        String token = servico(SEGREDO).gerarAcesso(UUID.randomUUID(), "a@b.com", Papel.ADMIN);
        JwtService outro = servico("um-outro-segredo-completamente-diferente-e-longo");
        assertThrows(JwtException.class, () -> outro.ler(token));
    }

    @Test
    @DisplayName("segredo curto derruba a aplicação na subida")
    void segredoCurtoNaoSobe() {
        assertThrows(IllegalStateException.class, () -> servico("curto-demais"));
    }
}
