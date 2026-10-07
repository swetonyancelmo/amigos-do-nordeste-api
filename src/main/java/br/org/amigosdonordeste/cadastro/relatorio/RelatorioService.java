package br.org.amigosdonordeste.cadastro.relatorio;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.comunidade.Comunidade;
import br.org.amigosdonordeste.cadastro.comunidade.ComunidadeRepositorio;
import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.familia.FamiliaDetalheResponse;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.PessoaResponse;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.municipio.Municipio;
import br.org.amigosdonordeste.cadastro.municipio.MunicipioRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.PessoaRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.relatorio.NecessidadesResponse.ItemContagem;
import br.org.amigosdonordeste.cadastro.relatorio.SituacaoResponse.Indicador;

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

  public RelatorioService(FamiliaRepositorio familiaRepositorio, PessoaRepositorio pessoaRepositorio,
                          ComunidadeRepositorio comunidadeRepositorio, MunicipioRepositorio municipioRepositorio) {
    this.familiaRepositorio = familiaRepositorio;
    this.pessoaRepositorio = pessoaRepositorio;
    this.comunidadeRepositorio = comunidadeRepositorio;
    this.municipioRepositorio = municipioRepositorio;
  }

  /**
   * Retorna os pontos no mapa agregados por comunidade e os dados do município
   * (quando o filtro de município for informado).
   */
  public PontosPorComunidadeResponse pontosPorComunidade(UUID municipioId) {
    // 1. Busca os pontos agregados no repositório (com base no filtro)
    List<PontoComunidadeResponse> pontos = municipioId != null
      ? comunidadeRepositorio.buscarPontosAgregadosPorMunicipio(municipioId)
      : comunidadeRepositorio.buscarPontosAgregados();

    // 2. Monta as informações do município se o filtro municipioId tiver sido informado
    MunicipioResponse municipioResponse = null;

    if (municipioId != null) {
      municipioResponse = municipioRepositorio.findById(municipioId)
        .map(m -> new MunicipioResponse(m.getId(), m.getNome(), m.getCodigoIbge()))
        .orElse(null);
    }

    return new PontosPorComunidadeResponse(municipioResponse, pontos);
  }

  public NecessidadesResponse necessidades(UUID comunidadeId, UUID municipioId, boolean todasIdades) {
    long totalFamilias = familiaRepositorio.contarParaRelatorioNecessidades(comunidadeId, municipioId);

    List<PessoaResponse> pessoas = pessoaRepositorio
      .buscarParaRelatorioNecessidades(comunidadeId, municipioId)
      .stream()
      .map(PessoaResponse::fromEntity)
      .toList();

    long totalCriancasAte12 = pessoas.stream().filter(RelatorioService::ateDozeAnos).count();

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
}
