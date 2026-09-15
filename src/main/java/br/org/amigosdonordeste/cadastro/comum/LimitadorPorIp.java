package br.org.amigosdonordeste.cadastro.comum;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Janela deslizante em memoria: no maximo N tentativas por IP a cada janela.
 *
 * E de proposito simples — sem Redis, sem biblioteca. A API roda em uma
 * instancia so (ADR-0004), entao um mapa na JVM basta. Reiniciar o processo
 * zera o contador, e isso e aceitavel para o que se quer aqui: impedir que
 * alguem varra codigos de seis digitos por tentativa.
 */
public class LimitadorPorIp {

    private final int maxTentativas;
    private final Duration janela;
    private final Map<String, Deque<Instant>> tentativas = new ConcurrentHashMap<>();

    public LimitadorPorIp(int maxTentativas, Duration janela) {
        this.maxTentativas = maxTentativas;
        this.janela = janela;
    }

    /** Registra uma tentativa do IP; lanca se ele ja passou do limite na janela. */
    public void registrar(String ip) {
        Instant agora = Instant.now();
        Deque<Instant> fila = tentativas.computeIfAbsent(ip, chave -> new ArrayDeque<>());
        synchronized (fila) {
            while (!fila.isEmpty() && fila.peekFirst().isBefore(agora.minus(janela))) {
                fila.pollFirst();
            }
            if (fila.size() >= maxTentativas) {
                throw new MuitasTentativasException();
            }
            fila.addLast(agora);
        }
    }
}
