package br.org.amigosdonordeste.cadastro.relatorio;

import br.org.amigosdonordeste.cadastro.relatorio.SituacaoResponse.Indicador;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.SituacaoSentinela;

import java.util.List;
import java.util.UUID;

/**
 * GET /api/relatorios/vulnerabilidade (ADR-0010): quantas famílias em cada
 * estrato, no total, por município e por comunidade. Só contagens: nenhum
 * nome, nenhuma lista de famílias. DADOS_INSUFICIENTES sempre aparece, com o
 * que mais falta preencher, porque é a parte do relatório que pede ação.
 *
 * Os percentuais são sobre o total de famílias ativas do escopo.
 */
public record VulnerabilidadeResponse(
        long totalFamilias,
        int escoreMaximoAlcancavel,
        List<ContagemEstrato> distribuicao,
        List<PorMunicipio> porMunicipio,
        List<PorComunidade> porComunidade,
        List<CampoFaltante> camposFaltantes,
        List<SentinelaNaoAvaliada> sentinelasNaoAvaliadas
) {

    /** Todo estrato da base aparece, mesmo com zero, na ordem de prioridade. */
    public record ContagemEstrato(EstratoRisco estrato, String rotulo, long valor, double percentual) {
        static ContagemEstrato de(EstratoRisco estrato, String rotulo, long valor, long total) {
            Indicador indicador = Indicador.de(valor, total);
            return new ContagemEstrato(estrato, rotulo, indicador.valor(), indicador.percentual());
        }
    }

    public record PorMunicipio(UUID municipioId, String municipioNome, long totalFamilias,
                               List<ContagemEstrato> distribuicao) { }

    public record PorComunidade(UUID comunidadeId, String comunidadeNome, String municipioNome,
                                long totalFamilias, List<ContagemEstrato> distribuicao) { }

    /** Em quantas famílias de DADOS_INSUFICIENTES falta cada campo (mais frequente primeiro). */
    public record CampoFaltante(String campo, long familias) { }

    /** Sentinela do instrumento fora do escore, e por quê: o limite da avaliação, dito por escrito. */
    public record SentinelaNaoAvaliada(String codigo, String nome, Integer pontos, SituacaoSentinela situacao,
                                       String justificativa) { }
}
