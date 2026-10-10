package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base de conhecimento já carregada do banco (V15/V16) e validada: as
 * sentinelas avaliadas com peso, faixas e parâmetros; as que não entram, com
 * o motivo; e os estratos com corte e rótulo. É dado, não código: o motor só
 * aplica o que está aqui (ADR-0010).
 *
 * Valida tudo no construtor. Uma base que não fecha nunca chega ao motor.
 */
public record BaseDeConhecimento(
        List<Regra> regras,
        List<SentinelaNaoAvaliada> naoAvaliadas,
        List<CorteEstrato> estratos
) {

    /** Uma sentinela que entra no escore. */
    public sealed interface Regra permits RegraBinaria, RegraFaixa {
        String codigo();
        String nome();
        Parametros parametros();
        /** O máximo que a sentinela pode somar: é o que fica "em aberto" quando ela é indeterminada. */
        int pontosMaximos();
    }

    public record RegraBinaria(String codigo, String nome, int pontos, Parametros parametros) implements Regra {
        @Override
        public int pontosMaximos() {
            return pontos;
        }
    }

    public record RegraFaixa(String codigo, String nome, List<Faixa> faixas, Parametros parametros) implements Regra {
        public RegraFaixa {
            faixas = List.copyOf(faixas);
        }

        @Override
        public int pontosMaximos() {
            return faixas.stream().mapToInt(Faixa::pontos).max().orElse(0);
        }
    }

    /** "numerador/denominador OPERADOR limite → pontos". */
    public record Faixa(OperadorFaixa operador, BigDecimal limite, int pontos) {

        /** Compara sem dividir: n/d ? L  ⇔  n ? L·d (d > 0). Nada de arredondamento em "igual a 1". */
        boolean aceita(int numerador, int denominador) {
            BigDecimal lado = limite.multiply(BigDecimal.valueOf(denominador));
            return operador.aceita(BigDecimal.valueOf(numerador).compareTo(lado));
        }
    }

    /** Sentinela do instrumento que não entra no escore, e por quê. */
    public record SentinelaNaoAvaliada(
            String codigo, String nome, Integer pontos, SituacaoSentinela situacao, String justificativa) { }

    /** Corte e rótulo de um estrato. escoreMinimo é null só em DADOS_INSUFICIENTES. */
    public record CorteEstrato(
            EstratoRisco estrato, Integer escoreMinimo, String rotulo, String descricaoInstrumento, int ordem) { }

    public BaseDeConhecimento {
        regras = List.copyOf(regras);
        naoAvaliadas = List.copyOf(naoAvaliadas);
        estratos = estratos.stream().sorted(Comparator.comparingInt(CorteEstrato::ordem)).toList();
        validarRegras(regras);
        validarEstratos(estratos);
    }

    /**
     * Teto do escore com a base atual: soma do máximo de cada sentinela
     * avaliada. Com a V16 e o campo de cômodos, 10; sem cômodos, 7 (ADR-0010).
     */
    public int escoreMaximoAlcancavel() {
        return regras.stream().mapToInt(Regra::pontosMaximos).sum();
    }

    public CorteEstrato corte(EstratoRisco estrato) {
        return estratos.stream()
                .filter(c -> c.estrato() == estrato)
                .findFirst()
                .orElseThrow();
    }

    /** O estrato de um escore: o de maior corte que o escore alcança. */
    public CorteEstrato estratoDoEscore(int escore) {
        return estratos.stream()
                .filter(c -> c.escoreMinimo() != null && c.escoreMinimo() <= escore)
                .max(Comparator.comparingInt(CorteEstrato::escoreMinimo))
                .orElseThrow();
    }

    private static void validarRegras(List<Regra> regras) {
        Set<String> codigos = new HashSet<>();
        for (Regra regra : regras) {
            if (!codigos.add(regra.codigo())) {
                throw new BaseDeConhecimentoInvalidaException("sentinela repetida: " + regra.codigo());
            }
            switch (regra) {
                case RegraBinaria binaria -> {
                    if (binaria.pontos() < 0) {
                        throw new BaseDeConhecimentoInvalidaException(regra.codigo() + ": pontos negativos");
                    }
                    Avaliadores.binario(binaria.codigo()).validar(binaria.parametros());
                }
                case RegraFaixa faixa -> {
                    if (faixa.faixas().isEmpty()) {
                        throw new BaseDeConhecimentoInvalidaException(regra.codigo() + ": sentinela de faixa sem faixas");
                    }
                    Avaliadores.deFaixa(faixa.codigo());
                }
            }
        }
    }

    private static void validarEstratos(List<CorteEstrato> estratos) {
        Map<EstratoRisco, CorteEstrato> porCodigo = new EnumMap<>(EstratoRisco.class);
        Set<Integer> cortes = new HashSet<>();
        for (CorteEstrato corte : estratos) {
            if (porCodigo.put(corte.estrato(), corte) != null) {
                throw new BaseDeConhecimentoInvalidaException("estrato repetido: " + corte.estrato());
            }
            boolean deveTerCorte = corte.estrato() != EstratoRisco.DADOS_INSUFICIENTES;
            if (deveTerCorte != (corte.escoreMinimo() != null)) {
                throw new BaseDeConhecimentoInvalidaException(corte.estrato() + ": só DADOS_INSUFICIENTES fica sem corte");
            }
            if (corte.escoreMinimo() != null && !cortes.add(corte.escoreMinimo())) {
                throw new BaseDeConhecimentoInvalidaException("dois estratos com o mesmo corte: " + corte.escoreMinimo());
            }
        }
        if (porCodigo.size() != EstratoRisco.values().length) {
            throw new BaseDeConhecimentoInvalidaException("faltam estratos; há " + porCodigo.keySet());
        }
        if (porCodigo.get(EstratoRisco.SEM_RISCO_IDENTIFICADO).escoreMinimo() != 0) {
            throw new BaseDeConhecimentoInvalidaException("SEM_RISCO_IDENTIFICADO precisa começar em 0");
        }
        // a ordem dos cortes tem que seguir a do instrumento: R1 < R2 < R3
        int r1 = porCodigo.get(EstratoRisco.R1).escoreMinimo();
        int r2 = porCodigo.get(EstratoRisco.R2).escoreMinimo();
        int r3 = porCodigo.get(EstratoRisco.R3).escoreMinimo();
        if (!(0 < r1 && r1 < r2 && r2 < r3)) {
            throw new BaseDeConhecimentoInvalidaException("cortes fora de ordem: R1=" + r1 + ", R2=" + r2 + ", R3=" + r3);
        }
    }
}
