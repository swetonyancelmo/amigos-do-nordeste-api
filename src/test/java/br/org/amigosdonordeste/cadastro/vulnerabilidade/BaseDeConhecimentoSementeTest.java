package br.org.amigosdonordeste.cadastro.vulnerabilidade;

import br.org.amigosdonordeste.cadastro.vulnerabilidade.base.BaseDeConhecimentoService;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Faixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Regra;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraBinaria;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraFaixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.SentinelaNaoAvaliada;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.EstratoRisco;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.OperadorFaixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.SituacaoSentinela;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A V16 confere com o artigo de Coelho e Savassi (2004): nomes e pontos da
 * Tabela 01, cortes do Quadro 02. Se alguém "ajustar" peso ou corte na
 * migração, este teste quebra, e a mudança tem que passar pela ADR-0010.
 *
 * Lê a base pelo mesmo caminho da produção (BaseDeConhecimentoService); no
 * perfil de teste a V16 é a semente do H2.
 */
@SpringBootTest
@ActiveProfiles("test")
class BaseDeConhecimentoSementeTest {

    @Autowired BaseDeConhecimentoService baseDeConhecimento;

    @Test
    @DisplayName("as 13 sentinelas da Tabela 01 estão na base, com o nome e os pontos do artigo")
    void sentinelasDoArtigo() {
        BaseDeConhecimento base = baseDeConhecimento.carregar();

        Map<String, Integer> pontosPorNome = base.regras().stream()
                .filter(r -> r instanceof RegraBinaria)
                .collect(Collectors.toMap(Regra::nome, Regra::pontosMaximos));
        base.naoAvaliadas().forEach(s -> pontosPorNome.put(s.nome(), s.pontos()));

        assertEquals(Map.ofEntries(
                Map.entry("Acamado", 3),
                Map.entry("Deficiência Física", 3),
                Map.entry("Deficiência mental", 3),
                Map.entry("Baixas condições de saneamento", 3),
                Map.entry("Desnutrição (Grave)", 3),
                Map.entry("Drogadição", 2),
                Map.entry("Desemprego", 2),
                Map.entry("Analfabetismo", 1),
                Map.entry("Menor de seis meses", 1),
                Map.entry("Maior de 70 anos", 1),
                Map.entry("Hipertensão Arterial Sistêmica", 1),
                Map.entry("Diabetes Mellitus", 1)), pontosPorNome);

        RegraFaixa razao = (RegraFaixa) base.regras().stream()
                .filter(r -> r.codigo().equals("RELACAO_MORADOR_COMODO")).findFirst().orElseThrow();
        assertEquals("Relação morador/cômodo", razao.nome());
        Map<OperadorFaixa, Integer> faixas = razao.faixas().stream()
                .collect(Collectors.toMap(Faixa::operador, Faixa::pontos));
        assertEquals(Map.of(OperadorFaixa.MAIOR, 3, OperadorFaixa.IGUAL, 2, OperadorFaixa.MENOR, 0), faixas);
        razao.faixas().forEach(f -> assertEquals(0, f.limite().compareTo(java.math.BigDecimal.ONE)));
    }

    @Test
    @DisplayName("as sentinelas de saúde ficam fora por LGPD, e analfabetismo fora por falta de campo")
    void cortesDaAdaptacao() {
        Map<String, SituacaoSentinela> situacoes = baseDeConhecimento.carregar().naoAvaliadas().stream()
                .collect(Collectors.toMap(SentinelaNaoAvaliada::codigo, SentinelaNaoAvaliada::situacao));

        assertEquals(Map.of(
                "ANALFABETISMO", SituacaoSentinela.NAO_COLETADA,
                "ACAMADO", SituacaoSentinela.DESCARTADA_LGPD,
                "DEFICIENCIA_FISICA", SituacaoSentinela.DESCARTADA_LGPD,
                "DEFICIENCIA_MENTAL", SituacaoSentinela.DESCARTADA_LGPD,
                "DESNUTRICAO_GRAVE", SituacaoSentinela.DESCARTADA_LGPD,
                "DROGADICAO", SituacaoSentinela.DESCARTADA_LGPD,
                "HIPERTENSAO_ARTERIAL", SituacaoSentinela.DESCARTADA_LGPD,
                "DIABETES_MELLITUS", SituacaoSentinela.DESCARTADA_LGPD), situacoes);
    }

    @Test
    @DisplayName("cortes do Quadro 02 sem reescala: R1 = 5, R2 = 7, R3 = 9; teto alcançável 10")
    void cortesOriginais() {
        BaseDeConhecimento base = baseDeConhecimento.carregar();

        assertEquals(5, base.corte(EstratoRisco.R1).escoreMinimo());
        assertEquals(7, base.corte(EstratoRisco.R2).escoreMinimo());
        assertEquals(9, base.corte(EstratoRisco.R3).escoreMinimo());
        assertEquals(0, base.corte(EstratoRisco.SEM_RISCO_IDENTIFICADO).escoreMinimo());
        assertNull(base.corte(EstratoRisco.DADOS_INSUFICIENTES).escoreMinimo());
        assertEquals(10, base.escoreMaximoAlcancavel());
    }
}
