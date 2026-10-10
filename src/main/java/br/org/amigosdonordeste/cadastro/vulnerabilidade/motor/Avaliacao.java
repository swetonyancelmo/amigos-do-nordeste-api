package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Resultado da avaliação de uma família, com a explicação (ADR-0010). É uma
 * SUGESTÃO para quem decide, nunca uma decisão: nada no sistema age sozinho
 * a partir dela. Calculada a cada leitura, nunca gravada (regra 2).
 */
@Schema(description = "Sugestão de prioridade pela Escala de Coelho-Savassi adaptada (ADR-0010). "
        + "Calculada na hora; quem decide é a pessoa.")
public record Avaliacao(
        @Schema(description = "R3, R2, R1, SEM_RISCO_IDENTIFICADO ou DADOS_INSUFICIENTES")
        EstratoRisco estrato,

        @Schema(description = "Texto para a tela, configurado na base de conhecimento")
        String rotulo,

        @Schema(description = "Soma dos pontos. null em DADOS_INSUFICIENTES: sem dado, não há escore")
        Integer escore,

        @Schema(description = "Pontos das sentinelas presentes (o mínimo que a família soma)")
        int pontosConfirmados,

        @Schema(description = "Quanto ainda pode somar se os dados que faltam forem preenchidos")
        int pontosEmAberto,

        @Schema(description = "Teto do escore com as sentinelas que o sistema avalia hoje")
        int escoreMaximoAlcancavel,

        @Schema(description = "Sentinelas que dispararam, com os pontos de cada uma")
        List<ItemExplicacao> sentinelasPresentes,

        @Schema(description = "Sentinelas com dado informado que não dispararam (somam 0)")
        List<ItemExplicacao> sentinelasAusentes,

        @Schema(description = "Sentinelas sem dado para decidir: não somam e não são 'ausentes'")
        List<ItemExplicacao> sentinelasIndeterminadas,

        @Schema(description = "Campos a completar para tirar as sentinelas da indeterminação")
        List<String> camposFaltantes
) {

    public record ItemExplicacao(
            String codigo,
            String nome,
            Constatacao constatacao,
            @Schema(description = "Pontos somados; 0 se ausente; null se indeterminada")
            Integer pontos,
            @Schema(description = "O máximo que esta sentinela soma")
            int pontosMaximos,
            String detalhe,
            List<String> camposFaltantes
    ) { }
}
