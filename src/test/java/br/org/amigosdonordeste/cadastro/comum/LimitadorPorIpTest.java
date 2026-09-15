package br.org.amigosdonordeste.cadastro.comum;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * O que estes testes protegem:
 *  - a janela desliza: passado o tempo, o IP volta a poder tentar;
 *  - IPs cuja janela passou saem do mapa (rota publica nao pode crescer
 *    memoria sem limite com enderecos distintos).
 */
class LimitadorPorIpTest {

    private final AtomicReference<Instant> agora = new AtomicReference<>(Instant.parse("2026-09-15T12:00:00Z"));
    private final Clock relogio = new Clock() {
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return agora.get(); }
    };

    private void avancar(Duration d) { agora.updateAndGet(i -> i.plus(d)); }

    @Test
    @DisplayName("barra a tentativa além do limite e libera quando a janela passa")
    void janelaDesliza() {
        LimitadorPorIp limitador = new LimitadorPorIp(2, Duration.ofMinutes(1), relogio);

        limitador.registrar("10.0.0.1");
        limitador.registrar("10.0.0.1");
        assertThrows(MuitasTentativasException.class, () -> limitador.registrar("10.0.0.1"));

        avancar(Duration.ofSeconds(61));
        assertDoesNotThrow(() -> limitador.registrar("10.0.0.1"));
    }

    @Test
    @DisplayName("IPs com a janela vencida são removidos do mapa")
    void removeIpsExpirados() {
        LimitadorPorIp limitador = new LimitadorPorIp(5, Duration.ofMinutes(1), relogio);

        for (int i = 0; i < 300; i++) {
            limitador.registrar("2001:db8::" + i);
        }
        assertTrue(limitador.ipsAcompanhados() > 0);

        avancar(Duration.ofMinutes(2));
        // Registros suficientes para disparar uma varredura com tudo vencido.
        for (int i = 0; i < 300; i++) {
            limitador.registrar("10.0." + (i / 256) + "." + (i % 256));
        }

        assertTrue(limitador.ipsAcompanhados() <= 300,
            "os 300 IPv6 antigos deveriam ter saido, mas ha " + limitador.ipsAcompanhados());
    }
}
