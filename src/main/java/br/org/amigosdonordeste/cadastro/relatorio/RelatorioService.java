package br.org.amigosdonordeste.cadastro.relatorio;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import br.org.amigosdonordeste.cadastro.familia.Familia;
import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.relatorio.dto.SituacaoResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.org.amigosdonordeste.cadastro.dominio.NumerosCalcado;
import br.org.amigosdonordeste.cadastro.familia.FamiliaRepositorio;
import br.org.amigosdonordeste.cadastro.familia.PessoaResponse;
import br.org.amigosdonordeste.cadastro.pessoa.PessoaRepositorio;
import br.org.amigosdonordeste.cadastro.pessoa.enums.FaixaEtaria;
import br.org.amigosdonordeste.cadastro.pessoa.enums.TamanhoRoupa;
import br.org.amigosdonordeste.cadastro.relatorio.NecessidadesResponse.ItemContagem;

/**
 * Issue #18: é este relatório que justifica o sistema inteiro — responde
 * "quantas peças tamanho M eu compro para o Sítio Igrejinha" sem contar à
 * mão. readOnly porque só lê.
 */
@Service
@Transactional(readOnly = true)
public class RelatorioService {

  private final FamiliaRepositorio familiaRepositorio;
  private final PessoaRepositorio pessoaRepositorio;

  public RelatorioService(FamiliaRepositorio familiaRepositorio, PessoaRepositorio pessoaRepositorio) {
    this.familiaRepositorio = familiaRepositorio;
    this.pessoaRepositorio = pessoaRepositorio;
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
    List<Familia> familias = familiaRepositorio.buscarParaRelatorioSituacao(comunidadeId, municipioId);
    long totalFamilias = familias.size();

    long semBanheiro = familias.stream()
      .filter(f -> !Boolean.TRUE.equals(f.getTemBanheiro()))
      .count();

    long soCarroPipa = familias.stream()
      .filter(f -> f.getAbastecimentoAgua() != null
        && f.getAbastecimentoAgua().size() == 1
        && f.getAbastecimentoAgua().contains(AbastecimentoAgua.CARRO_PIPA))
      .count();

    long soBolsaFamilia = familias.stream()
      .filter(f -> f.getFontesRenda() != null
        && f.getFontesRenda().size() == 1
        && f.getFontesRenda().stream()
        .anyMatch(fr -> fr.getTipo() == TipoFonteRenda.BOLSA_FAMILIA)) // ou fr.getFonteRenda() == FonteRendaEnum.BOLSA_FAMILIA
      .count();

    long semTratamentoAgua = familias.stream()
      .filter(f -> TratamentoAgua.SEM_TRATAMENTO.equals(f.getTratamentoAgua()))
      .count();

    return new SituacaoResponse(
      criarIndicador(semBanheiro, totalFamilias),
      criarIndicador(soCarroPipa, totalFamilias),
      criarIndicador(soBolsaFamilia, totalFamilias),
      criarIndicador(semTratamentoAgua, totalFamilias)
    );
  }

  private SituacaoResponse.Indicador criarIndicador(long valor, long total) {
    if (total == 0) {
      return new SituacaoResponse.Indicador(0, 0.0);
    }
    double percentual = ((double) valor / total) * 100.0;
    return new SituacaoResponse.Indicador(valor, Math.round(percentual * 100.0) / 100.0);
  }

}
