package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao.ItemExplicacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliadores.Leitura;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliadores.Medida;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.CorteEstrato;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Faixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Regra;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraBinaria;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraFaixa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Motor de inferência (ADR-0010): lê as regras da base, avalia a família,
 * soma, estratifica e explica. Puro: sem banco, sem relógio (o "hoje" entra
 * por parâmetro), sem estado. Mesma família + mesma base + mesmo dia = mesmo
 * resultado.
 *
 * DADOS_INSUFICIENTES (ADR-0010, regra 1): o estrato só é dado quando os
 * dados que faltam não poderiam mudá-lo. O motor calcula o estrato com as
 * indeterminadas valendo zero e com elas valendo o máximo; se os dois
 * diferem, a família não recebe escore nem estrato, e sim
 * DADOS_INSUFICIENTES com a lista do que completar. Ausência de dado nunca
 * vira "sem risco".
 */
public final class MotorDeInferencia {

    private MotorDeInferencia() { }

    public static Avaliacao avaliar(FatosFamilia fatos, BaseDeConhecimento base, LocalDate hoje) {
        List<ItemExplicacao> presentes = new ArrayList<>();
        List<ItemExplicacao> ausentes = new ArrayList<>();
        List<ItemExplicacao> indeterminadas = new ArrayList<>();
        Set<String> camposFaltantes = new LinkedHashSet<>();
        int confirmados = 0;
        int emAberto = 0;

        for (Regra regra : base.regras()) {
            ItemExplicacao item = switch (regra) {
                case RegraBinaria binaria -> avaliarBinaria(binaria, fatos, hoje);
                case RegraFaixa faixa -> avaliarFaixa(faixa, fatos);
            };
            switch (item.constatacao()) {
                case PRESENTE -> {
                    confirmados += item.pontos();
                    presentes.add(item);
                }
                case AUSENTE -> ausentes.add(item);
                case INDETERMINADA -> {
                    emAberto += item.pontosMaximos();
                    indeterminadas.add(item);
                    camposFaltantes.addAll(item.camposFaltantes());
                }
            }
        }

        CorteEstrato noMinimo = base.estratoDoEscore(confirmados);
        CorteEstrato noMaximo = base.estratoDoEscore(confirmados + emAberto);
        boolean decidido = noMinimo.estrato() == noMaximo.estrato();
        CorteEstrato estrato = decidido ? noMinimo : base.corte(EstratoRisco.DADOS_INSUFICIENTES);

        return new Avaliacao(
                estrato.estrato(),
                estrato.rotulo(),
                decidido ? confirmados : null,
                confirmados,
                emAberto,
                base.escoreMaximoAlcancavel(),
                List.copyOf(presentes),
                List.copyOf(ausentes),
                List.copyOf(indeterminadas),
                List.copyOf(camposFaltantes));
    }

    private static ItemExplicacao avaliarBinaria(RegraBinaria regra, FatosFamilia fatos, LocalDate hoje) {
        Leitura leitura = Avaliadores.binario(regra.codigo()).avaliar(fatos, regra.parametros(), hoje);
        Integer pontos = switch (leitura.constatacao()) {
            case PRESENTE -> regra.pontos();
            case AUSENTE -> 0;
            case INDETERMINADA -> null;
        };
        return new ItemExplicacao(regra.codigo(), regra.nome(), leitura.constatacao(), pontos,
                regra.pontosMaximos(), leitura.detalhe(), leitura.faltando());
    }

    /**
     * Faixa: pontua a faixa que a razão alcança. Se mais de uma servir (base
     * editada com faixas sobrepostas), vale a de mais pontos; se nenhuma
     * servir, 0 — e o detalhe diz isso.
     */
    private static ItemExplicacao avaliarFaixa(RegraFaixa regra, FatosFamilia fatos) {
        Medida medida = Avaliadores.deFaixa(regra.codigo()).medir(fatos);
        if (medida.indeterminada()) {
            return new ItemExplicacao(regra.codigo(), regra.nome(), Constatacao.INDETERMINADA, null,
                    regra.pontosMaximos(), medida.detalhe(), medida.faltando());
        }
        Faixa faixa = regra.faixas().stream()
                .filter(f -> f.aceita(medida.numerador(), medida.denominador()))
                .max((a, b) -> Integer.compare(a.pontos(), b.pontos()))
                .orElse(null);
        int pontos = faixa == null ? 0 : faixa.pontos();
        String detalhe = medida.detalhe() + (faixa == null
                ? " (nenhuma faixa da base se aplica)"
                : " (" + descrever(faixa) + ")");
        return new ItemExplicacao(regra.codigo(), regra.nome(),
                pontos > 0 ? Constatacao.PRESENTE : Constatacao.AUSENTE, pontos,
                regra.pontosMaximos(), detalhe, List.of());
    }

    private static String descrever(Faixa faixa) {
        String limite = faixa.limite().stripTrailingZeros().toPlainString();
        return switch (faixa.operador()) {
            case MAIOR -> "relação maior que " + limite;
            case IGUAL -> "relação igual a " + limite;
            case MENOR -> "relação menor que " + limite;
        };
    }
}
