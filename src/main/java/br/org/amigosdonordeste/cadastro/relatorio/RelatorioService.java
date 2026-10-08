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
import br.org.amigosdonordeste.cadastro.municipio.MunicipioNaoEncontradoException;
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
}
