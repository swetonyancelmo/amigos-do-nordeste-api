package br.org.amigosdonordeste.cadastro.vulnerabilidade.base;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.BaseDeConhecimentoResposta;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.CorteEstrato;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Faixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Regra;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraBinaria;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraFaixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.SentinelaNaoAvaliada;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimentoInvalidaException;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Parametros;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.SituacaoSentinela;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.TipoSentinela;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Lê a base de conhecimento do banco a cada chamada, sem cache: um UPDATE
 * num peso vale na próxima requisição, sem recompilar nem reiniciar
 * (ADR-0010). São poucas dezenas de linhas; uma leitura por requisição, não
 * por família.
 */
@Service
@Transactional(readOnly = true)
public class BaseDeConhecimentoService {

    private final RegraSentinelaRepositorio sentinelas;
    private final RegraEstratoRepositorio estratos;

    public BaseDeConhecimentoService(RegraSentinelaRepositorio sentinelas, RegraEstratoRepositorio estratos) {
        this.sentinelas = sentinelas;
        this.estratos = estratos;
    }

    public BaseDeConhecimento carregar() {
        return montar(sentinelas.buscarBaseCompleta(), estratos.findAllByOrderByOrdemAsc());
    }

    /**
     * A base em uso, para a tela explicar de onde vem a prioridade
     * (GET /api/vulnerabilidade/base). Passa pela mesma validação do motor:
     * a tela nunca mostra uma regra que o cálculo não usaria.
     */
    public BaseDeConhecimentoResposta descrever() {
        List<RegraSentinela> linhas = sentinelas.buscarBaseCompleta();
        BaseDeConhecimento base = montar(linhas, estratos.findAllByOrderByOrdemAsc());

        Map<String, String> criterios = new HashMap<>();
        linhas.forEach(s -> criterios.put(s.getCodigo(), s.getJustificativa()));

        List<BaseDeConhecimentoResposta.SentinelaAvaliada> avaliadas = base.regras().stream()
                .map(r -> switch (r) {
                    case RegraBinaria b -> new BaseDeConhecimentoResposta.SentinelaAvaliada(
                            b.codigo(), b.nome(), TipoSentinela.BINARIA, b.pontos(), List.of(), criterios.get(b.codigo()));
                    case RegraFaixa f -> new BaseDeConhecimentoResposta.SentinelaAvaliada(
                            f.codigo(), f.nome(), TipoSentinela.FAIXA, null,
                            f.faixas().stream()
                                    .map(x -> new BaseDeConhecimentoResposta.Faixa(x.operador(), x.limite(), x.pontos()))
                                    .toList(),
                            criterios.get(f.codigo()));
                })
                .toList();

        // faixa de escore de cada estrato: do seu corte até o corte seguinte - 1
        List<Integer> cortes = base.estratos().stream()
                .map(CorteEstrato::escoreMinimo)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
        List<BaseDeConhecimentoResposta.Estrato> estratosResposta = base.estratos().stream()
                .map(c -> {
                    Integer maximo = null;
                    if (c.escoreMinimo() != null) {
                        int i = cortes.indexOf(c.escoreMinimo());
                        maximo = i + 1 < cortes.size() ? cortes.get(i + 1) - 1 : null;
                    }
                    return new BaseDeConhecimentoResposta.Estrato(c.estrato(), c.rotulo(), c.descricaoInstrumento(),
                            c.escoreMinimo(), maximo, c.ordem());
                })
                .toList();

        List<BaseDeConhecimentoResposta.SentinelaForaDaConta> fora = base.naoAvaliadas().stream()
                .map(s -> new BaseDeConhecimentoResposta.SentinelaForaDaConta(
                        s.codigo(), s.nome(), s.pontos(), s.situacao(), s.justificativa()))
                .toList();

        return new BaseDeConhecimentoResposta(base.escoreMaximoAlcancavel(), avaliadas, estratosResposta, fora);
    }

    private static BaseDeConhecimento montar(List<RegraSentinela> linhas, List<RegraEstrato> linhasEstrato) {
        List<Regra> regras = new ArrayList<>();
        List<SentinelaNaoAvaliada> naoAvaliadas = new ArrayList<>();

        for (RegraSentinela s : linhas) {
            if (s.getSituacao() != SituacaoSentinela.AVALIADA) {
                naoAvaliadas.add(new SentinelaNaoAvaliada(
                        s.getCodigo(), s.getNome(), s.getPontos(), s.getSituacao(), s.getJustificativa()));
                continue;
            }
            Parametros parametros = new Parametros(s.getCodigo(), agrupar(s));
            if (s.getTipo() == TipoSentinela.BINARIA) {
                if (s.getPontos() == null) {
                    throw new BaseDeConhecimentoInvalidaException(s.getCodigo() + ": sentinela binária sem pontos");
                }
                regras.add(new RegraBinaria(s.getCodigo(), s.getNome(), s.getPontos(), parametros));
            } else {
                List<Faixa> faixas = s.getFaixas().stream()
                        .map(f -> new Faixa(f.getOperador(), f.getLimite(), f.getPontos()))
                        // ordem estável, para a explicação sair igual a cada leitura
                        .sorted(Comparator.comparing(Faixa::limite).thenComparing(Faixa::operador))
                        .toList();
                regras.add(new RegraFaixa(s.getCodigo(), s.getNome(), faixas, parametros));
            }
        }

        List<CorteEstrato> cortes = linhasEstrato.stream()
                .map(e -> new CorteEstrato(estrato(e.getCodigo()), e.getEscoreMinimo(), e.getRotulo(),
                        e.getDescricaoInstrumento(), e.getOrdem()))
                .toList();

        return new BaseDeConhecimento(regras, naoAvaliadas, cortes);
    }

    private static Map<String, List<String>> agrupar(RegraSentinela s) {
        Map<String, List<String>> porNome = new LinkedHashMap<>();
        s.getParametros().stream()
                .sorted(Comparator.comparing(RegraSentinela.ParametroSentinela::getNome)
                        .thenComparing(RegraSentinela.ParametroSentinela::getValor))
                .forEach(p -> porNome.computeIfAbsent(p.getNome(), k -> new ArrayList<>()).add(p.getValor()));
        return porNome;
    }

    private static EstratoRisco estrato(String codigo) {
        try {
            return EstratoRisco.valueOf(codigo);
        } catch (IllegalArgumentException e) {
            throw new BaseDeConhecimentoInvalidaException("estrato desconhecido: " + codigo);
        }
    }
}
