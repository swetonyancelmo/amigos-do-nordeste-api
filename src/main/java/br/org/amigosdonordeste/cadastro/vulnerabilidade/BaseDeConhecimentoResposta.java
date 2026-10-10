package br.org.amigosdonordeste.cadastro.vulnerabilidade;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.OperadorFaixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.SituacaoSentinela;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.TipoSentinela;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

/**
 * GET /api/vulnerabilidade/base: a base de conhecimento EM USO, para a tela
 * mostrar de onde vem a prioridade sugerida (ADR-0010). São os valores do
 * banco, já validados como o motor os usa. Se a associação ajustar um peso,
 * esta resposta muda junto. Os valores originais do artigo ficam na ADR.
 *
 * Nenhum dado de família aqui: só as regras.
 */
@Schema(description = "Regras em uso na avaliação de vulnerabilidade (Escala de Coelho-Savassi adaptada)")
public record BaseDeConhecimentoResposta(
        @Schema(description = "Soma máxima possível com as sentinelas avaliadas")
        int escoreMaximoAlcancavel,

        @Schema(description = "Sentinelas que entram na conta, na ordem da base")
        List<SentinelaAvaliada> sentinelas,

        @Schema(description = "Estratos na ordem de prioridade, com a faixa de escore de cada um")
        List<Estrato> estratos,

        @Schema(description = "Sentinelas do instrumento que ficam fora da conta, e por quê")
        List<SentinelaForaDaConta> sentinelasNaoAvaliadas
) {

    public record SentinelaAvaliada(
            String codigo,
            @Schema(description = "Nome como no artigo") String nome,
            TipoSentinela tipo,
            @Schema(description = "Pontos de uma BINARIA; null numa FAIXA (os pontos estão em faixas)") Integer pontos,
            @Schema(description = "Só nas de FAIXA: razão OPERADOR limite → pontos") List<Faixa> faixas,
            @Schema(description = "Como o cadastro é lido para esta sentinela (adaptação da ADR-0010)") String criterio
    ) { }

    public record Faixa(OperadorFaixa operador, BigDecimal limite, int pontos) { }

    public record Estrato(
            EstratoRisco estrato,
            @Schema(description = "Texto de exibição, configurável") String rotulo,
            @Schema(description = "O que o estrato é no instrumento") String descricaoInstrumento,
            @Schema(description = "Escore mínimo; null em DADOS_INSUFICIENTES") Integer escoreMinimo,
            @Schema(description = "Escore máximo; null no estrato mais alto e em DADOS_INSUFICIENTES") Integer escoreMaximo,
            int ordem
    ) { }

    public record SentinelaForaDaConta(
            String codigo,
            String nome,
            @Schema(description = "Pontos que ela teria no instrumento") Integer pontos,
            SituacaoSentinela situacao,
            String justificativa
    ) { }
}
