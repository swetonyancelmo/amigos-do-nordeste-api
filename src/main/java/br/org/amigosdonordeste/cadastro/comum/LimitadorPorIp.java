package br.org.amigosdonordeste.cadastro.comum;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Janela deslizante em memoria: no maximo N tentativas por IP a cada janela.
 *
 * E de proposito simples — sem Redis, sem biblioteca. A API roda em uma
 * instancia so (ADR-0004), entao um mapa na JVM basta. Reiniciar o processo
 * zera o contador, e isso e aceitavel para o que se quer aqui: impedir que
 * alguem varra codigos de seis digitos por tentativa.
 *
 * Os metodos sao synchronized: a rota e rara (uma ativacao por aparelho) e a
 * simplicidade vale mais que paralelismo. Isso tambem torna a varredura de
 * IPs expirados segura, sem corrida entre quem registra e quem remove.
 */
public class LimitadorPorIp {

    /** A cada tantos registros, remove do mapa os IPs cuja janela ja passou. */
    private static final int REGISTROS_ENTRE_VARREDURAS = 256;

    private final int maxTentativas;
    private final Duration janela;
    private final Clock relogio;
    private final Map<String, Deque<Instant>> tentativas = new HashMap<>();
    private int registrosDesdeVarredura;

    public LimitadorPorIp(int maxTentativas, Duration janela) {
        this(maxTentativas, janela, Clock.systemUTC());
    }

    LimitadorPorIp(int maxTentativas, Duration janela, Clock relogio) {
        this.maxTentativas = maxTentativas;
        this.janela = janela;
        this.relogio = relogio;
    }

    /** Registra uma tentativa do IP; lanca se ele ja passou do limite na janela. */
    public synchronized void registrar(String ip) {
        Instant agora = relogio.instant();
        Instant corte = agora.minus(janela);

        if (++registrosDesdeVarredura >= REGISTROS_ENTRE_VARREDURAS) {
            varrer(corte);
        }

        Deque<Instant> fila = tentativas.computeIfAbsent(ip, chave -> new ArrayDeque<>());
        while (!fila.isEmpty() && fila.peekFirst().isBefore(corte)) {
            fila.pollFirst();
        }
        if (fila.size() >= maxTentativas) {
            throw new MuitasTentativasException();
        }
        fila.addLast(agora);
    }

    /** Quantos IPs o limitador esta acompanhando agora. */
    public synchronized int ipsAcompanhados() {
        return tentativas.size();
    }

    /**
     * Sem isto, cada IP novo ficaria no mapa para sempre: a rota e publica, e
     * uma enxurrada de enderecos distintos (IPv6, por exemplo) cresceria a
     * memoria sem limite mesmo depois de todas as janelas passarem.
     */
    private void varrer(Instant corte) {
        tentativas.values().removeIf(fila -> fila.isEmpty() || fila.peekLast().isBefore(corte));
        registrosDesdeVarredura = 0;
    }
}
