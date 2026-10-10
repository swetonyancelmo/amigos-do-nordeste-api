package br.org.amigosdonordeste.cadastro.relatorio;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.FamiliaDetalheResponse;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.PessoaResponse;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.PessoaRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.relatorio.NecessidadesResponse.ItemContagem;
import br.org.amigosdonordeste.cadastro.relatorio.SituacaoResponse.Indicador;
import br.org.amigosdonordeste.cadastro.relatorio.VulnerabilidadeResponse.CampoFaltante;
import br.org.amigosdonordeste.cadastro.relatorio.VulnerabilidadeResponse.ContagemEstrato;
import br.org.amigosdonordeste.cadastro.relatorio.VulnerabilidadeResponse.PorComunidade;
import br.org.amigosdonordeste.cadastro.relatorio.VulnerabilidadeResponse.PorMunicipio;
import br.org.amigosdonordeste.cadastro.relatorio.VulnerabilidadeResponse.SentinelaNaoAvaliada;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.AvaliacaoVulnerabilidadeService;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.CorteEstrato;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;

/**
 * Issue #18: é este relatório que justifica o sistema inteiro — responde
 * "quantas peças tamanho M eu compro para o Sítio Igrejinha" sem contar à
 * mão. Issue #19: os indicadores de situação das famílias. readOnly porque
 * só lê.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

    private final FamiliaRepositorio familiaRepositorio;
    private final PessoaRepositorio pessoaRepositorio;
    private final ComunidadeRepositorio comunidadeRepositorio;
    private final MunicipioRepositorio municipioRepositorio;
    private final AvaliacaoVulnerabilidadeService avaliacaoVulnerabilidade;

    public RelatorioService(FamiliaRepositorio familiaRepositorio, PessoaRepositorio pessoaRepositorio,
            ComunidadeRepositorio comunidadeRepositorio, MunicipioRepositorio municipioRepositorio,
            AvaliacaoVulnerabilidadeService avaliacaoVulnerabilidade) {
        this.familiaRepositorio = familiaRepositorio;
        this.pessoaRepositorio = pessoaRepositorio;
        this.comunidadeRepositorio = comunidadeRepositorio;
        this.municipioRepositorio = municipioRepositorio;
        this.avaliacaoVulnerabilidade = avaliacaoVulnerabilidade;
    }

  /** Mapa: um ponto por comunidade; com municipioId, devolve também o município (e o código IBGE). */
  public PontosPorComunidadeResponse mapa(UUID municipioId) {
    MunicipioResponse municipio = null;
    if (municipioId != null) {
      Municipio m = municipioRepositorio.findById(municipioId)
        .orElseThrow(() -> new MunicipioNaoEncontradoException(municipioId));
      municipio = new MunicipioResponse(m.getId(), m.getNome(), m.getCodigoIbge());
    }
    return new PontosPorComunidadeResponse(municipio, comunidadeRepositorio.contarFamiliasPorComunidade(municipioId));
  }

  public NecessidadesResponse necessidades(UUID comunidadeId, UUID municipioId, boolean todasIdades) {
        long totalFamilias = familiaRepositorio.contarParaRelatorioNecessidades(comunidadeId, municipioId);

        List<PessoaResponse> pessoas = pessoaRepositorio
                .buscarParaRelatorioNecessidades(comunidadeId, municipioId)
                .stream()
                .map(PessoaResponse::fromEntity)
                .toList();

        long totalCriancasAte12 = pessoas.stream().filter(RelatorioService::ateDozeAnos).count();

        // todasIdades so afeta a contagem de roupa/calcado; totalPessoas e
        // totalCriancasAte12 sao sempre sobre todo mundo no escopo.
        List<PessoaResponse> pessoasContadas = todasIdades
                ? pessoas
                : pessoas.stream().filter(RelatorioService::ateDozeAnos).toList();

        return new NecessidadesResponse(
                totalFamilias,
                pessoas.size(),
                totalCriancasAte12,
                contarRoupa(pessoasContadas),
                contarCalcado(pessoasContadas),
                pessoasContadas.stream().filter(p -> p.tamanhoRoupa() == null).count(),
                pessoasContadas.stream().filter(p -> p.numeroCalcado() == null).count(),
                pessoas.stream().filter(p -> p.idade() == null).count());
    }

    /**
     * Issue #21: o mesmo relatório de necessidades, exportado em .xlsx com
     * abas extras (Famílias, Pessoas) para servir de backup — ver
     * requisitos.md RF-05. todasIdades aqui só afeta a aba "Necessidades",
     * igual ao endpoint JSON; Famílias e Pessoas sempre trazem todo mundo no
     * escopo, porque são a cópia de segurança do cadastro.
     */
    public PlanilhaGerada necessidadesXlsx(UUID comunidadeId, UUID municipioId, boolean todasIdades) {
        NecessidadesResponse necessidades = necessidades(comunidadeId, municipioId, todasIdades);

        List<FamiliaDetalheResponse> familias = familiaRepositorio
                .buscarParaExportacaoNecessidades(comunidadeId, municipioId)
                .stream()
                .map(FamiliaDetalheResponse::fromEntity)
                .toList();

        byte[] conteudo = RelatorioExcelBuilder.gerar(necessidades, familias);
        return new PlanilhaGerada(conteudo, nomeArquivoNecessidades(comunidadeId, municipioId));
    }

    private String nomeArquivoNecessidades(UUID comunidadeId, UUID municipioId) {
        String escopo = escopoNoNomeDoArquivo(comunidadeId, municipioId).trim().replaceAll("\\s+", "-");
        return "necessidades-" + escopo + "-" + LocalDate.now() + ".xlsx";
    }

    // comunidadeId manda no nome quando os dois filtros vierem — e o filtro
    // mais especifico, igual a regra de necessidades() (regra 8 do projeto:
    // mapa/relatorio sao por comunidade).
    private String escopoNoNomeDoArquivo(UUID comunidadeId, UUID municipioId) {
        if (comunidadeId != null) {
            return comunidadeRepositorio.findById(comunidadeId).map(Comunidade::getNome).orElse("comunidade");
        }
        if (municipioId != null) {
            return municipioRepositorio.findById(municipioId).map(Municipio::getNome).orElse("municipio");
        }
        return "geral";
    }

    private static boolean ateDozeAnos(PessoaResponse pessoa) {
        return FaixaEtaria.de(pessoa.idade()) == FaixaEtaria.ATE_12;
    }

    private static List<ItemContagem> contarRoupa(List<PessoaResponse> pessoas) {
        Map<TamanhoRoupa, Long> contagem = new LinkedHashMap<>();
        for (PessoaResponse pessoa : pessoas) {
            if (pessoa.tamanhoRoupa() != null) {
                contagem.merge(pessoa.tamanhoRoupa(), 1L, Long::sum);
            }
        }
        return Arrays.stream(TamanhoRoupa.values())
                .filter(contagem::containsKey)
                .map(tamanho -> new ItemContagem(tamanho.name(), contagem.get(tamanho)))
                .toList();
    }

    private static List<ItemContagem> contarCalcado(List<PessoaResponse> pessoas) {
        Map<String, Long> contagem = new LinkedHashMap<>();
        for (PessoaResponse pessoa : pessoas) {
            if (pessoa.numeroCalcado() != null) {
                contagem.merge(pessoa.numeroCalcado(), 1L, Long::sum);
            }
        }
        return NumerosCalcado.VALORES.stream()
                .filter(contagem::containsKey)
                .map(numero -> new ItemContagem(numero, contagem.get(numero)))
                .toList();
    }

    public SituacaoResponse situacao(UUID comunidadeId, UUID municipioId) {
        long totalFamilias = familiaRepositorio.contarParaRelatorioNecessidades(comunidadeId, municipioId);
        return new SituacaoResponse(
                totalFamilias,
                Indicador.de(familiaRepositorio.contarSemBanheiro(comunidadeId, municipioId), totalFamilias),
                Indicador.de(familiaRepositorio.contarSoComAbastecimento(
                        comunidadeId, municipioId, AbastecimentoAgua.CARRO_PIPA), totalFamilias),
                Indicador.de(familiaRepositorio.contarSoComFonteRenda(
                        comunidadeId, municipioId, TipoFonteRenda.BOLSA_FAMILIA), totalFamilias),
                Indicador.de(familiaRepositorio.contarPorTratamentoAgua(
                        comunidadeId, municipioId, TratamentoAgua.SEM_TRATAMENTO), totalFamilias));
    }

    /**
     * ADR-0010: distribuição das famílias ativas por estrato, no total, por
     * município e por comunidade. Avalia cada família na hora (o estrato não
     * é coluna). Mesmo escopo e mesma consulta da exportação de necessidades.
     * Só contagens saem daqui: nunca nome nem lista de famílias.
     */
    public VulnerabilidadeResponse vulnerabilidade(UUID comunidadeId, UUID municipioId) {
        BaseDeConhecimento base = avaliacaoVulnerabilidade.carregarBase();
        LocalDate hoje = LocalDate.now();

        List<Familia> familias = familiaRepositorio.buscarParaExportacaoNecessidades(comunidadeId, municipioId);

        Map<EstratoRisco, Long> total = new EnumMap<>(EstratoRisco.class);
        Map<UUID, Municipio> municipios = new LinkedHashMap<>();
        Map<UUID, Map<EstratoRisco, Long>> porMunicipio = new LinkedHashMap<>();
        Map<UUID, Comunidade> comunidades = new LinkedHashMap<>();
        Map<UUID, Map<EstratoRisco, Long>> porComunidade = new LinkedHashMap<>();
        Map<String, Long> faltas = new LinkedHashMap<>();

        for (Familia familia : familias) {
            Avaliacao avaliacao = AvaliacaoVulnerabilidadeService.avaliar(familia, base, hoje);
            EstratoRisco estrato = avaliacao.estrato();
            Comunidade comunidade = familia.getComunidade();
            Municipio municipio = comunidade.getMunicipio();

            total.merge(estrato, 1L, Long::sum);
            municipios.putIfAbsent(municipio.getId(), municipio);
            porMunicipio.computeIfAbsent(municipio.getId(), k -> new EnumMap<>(EstratoRisco.class))
                    .merge(estrato, 1L, Long::sum);
            comunidades.putIfAbsent(comunidade.getId(), comunidade);
            porComunidade.computeIfAbsent(comunidade.getId(), k -> new EnumMap<>(EstratoRisco.class))
                    .merge(estrato, 1L, Long::sum);
            if (estrato == EstratoRisco.DADOS_INSUFICIENTES) {
                avaliacao.camposFaltantes().forEach(campo -> faltas.merge(campo, 1L, Long::sum));
            }
        }

        List<PorMunicipio> linhasMunicipio = municipios.values().stream()
                .sorted(Comparator.comparing(Municipio::getNome))
                .map(m -> {
                    Map<EstratoRisco, Long> contagem = porMunicipio.get(m.getId());
                    long soma = somar(contagem);
                    return new PorMunicipio(m.getId(), m.getNome(), soma, distribuir(base, contagem, soma));
                })
                .toList();

        // a consulta já vem ordenada por nome da comunidade
        List<PorComunidade> linhasComunidade = comunidades.values().stream()
                .map(c -> {
                    Map<EstratoRisco, Long> contagem = porComunidade.get(c.getId());
                    long soma = somar(contagem);
                    return new PorComunidade(c.getId(), c.getNome(), c.getMunicipio().getNome(), soma,
                            distribuir(base, contagem, soma));
                })
                .toList();

        List<CampoFaltante> camposFaltantes = faltas.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .map(e -> new CampoFaltante(e.getKey(), e.getValue()))
                .toList();

        List<SentinelaNaoAvaliada> naoAvaliadas = base.naoAvaliadas().stream()
                .map(s -> new SentinelaNaoAvaliada(s.codigo(), s.nome(), s.pontos(), s.situacao(), s.justificativa()))
                .toList();

        return new VulnerabilidadeResponse(
                familias.size(),
                base.escoreMaximoAlcancavel(),
                distribuir(base, total, familias.size()),
                linhasMunicipio,
                linhasComunidade,
                camposFaltantes,
                naoAvaliadas);
    }

    private static long somar(Map<EstratoRisco, Long> contagem) {
        return contagem.values().stream().mapToLong(Long::longValue).sum();
    }

    private static List<ContagemEstrato> distribuir(BaseDeConhecimento base, Map<EstratoRisco, Long> contagem, long total) {
        return base.estratos().stream()
                .map((CorteEstrato c) -> ContagemEstrato.de(c.estrato(), c.rotulo(),
                        contagem.getOrDefault(c.estrato(), 0L), total))
                .toList();
    }
}
