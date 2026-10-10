package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.Avaliacao.ItemExplicacao;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.CorteEstrato;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.Faixa;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraBinaria;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.BaseDeConhecimento.RegraFaixa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O motor de inferência isolado: sem Spring, sem banco, com "hoje" fixo
 * (ADR-0010). A base daqui reproduz a V16 em Java para o motor ser testado
 * sozinho; que a V16 bate com o artigo é BaseDeConhecimentoSementeTest.
 *
 * Nenhum dado real: as famílias são só números e enums.
 */
class MotorDeInferenciaTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 9);

    // ------------------------------------------------------------ base e fatos

    /** Escala de Coelho-Savassi com a adaptação da ADR-0010 (mesmos valores da V16). */
    static BaseDeConhecimento baseCoelhoSavassi() {
        return baseCom(3);
    }

    static BaseDeConhecimento baseCom(int pontosSaneamento) {
        return new BaseDeConhecimento(
                List.of(
                        new RegraBinaria("BAIXAS_CONDICOES_SANEAMENTO", "Baixas condições de saneamento", pontosSaneamento,
                                new Parametros("BAIXAS_CONDICOES_SANEAMENTO", Map.of(
                                        "ESCOAMENTO_PRECARIO", List.of("CEU_ABERTO", "DIRETO_RIO_LAGO_MAR"),
                                        "TRATAMENTO_PRECARIO", List.of("SEM_TRATAMENTO"),
                                        "ABASTECIMENTO_SO_DE", List.of("CARRO_PIPA", "CAPTACAO_DIRETA_RIO")))),
                        new RegraBinaria("DESEMPREGO", "Desemprego", 2,
                                new Parametros("DESEMPREGO", Map.of(
                                        "FONTE_DE_TRABALHO", List.of("TRABALHO_FIXO", "TRABALHO_INFORMAL", "TRABALHO_SAZONAL"),
                                        "IDADE_ATIVA_MINIMA", List.of("18"),
                                        "IDADE_ATIVA_MAXIMA", List.of("59")))),
                        new RegraBinaria("MENOR_DE_SEIS_MESES", "Menor de seis meses", 1,
                                new Parametros("MENOR_DE_SEIS_MESES", Map.of("MESES", List.of("6")))),
                        new RegraBinaria("MAIOR_DE_70_ANOS", "Maior de 70 anos", 1,
                                new Parametros("MAIOR_DE_70_ANOS", Map.of("IDADE_MINIMA", List.of("70")))),
                        new RegraFaixa("RELACAO_MORADOR_COMODO", "Relação morador/cômodo", List.of(
                                new Faixa(OperadorFaixa.MAIOR, BigDecimal.ONE, 3),
                                new Faixa(OperadorFaixa.IGUAL, BigDecimal.ONE, 2),
                                new Faixa(OperadorFaixa.MENOR, BigDecimal.ONE, 0)),
                                new Parametros("RELACAO_MORADOR_COMODO", Map.of()))),
                List.of(),
                List.of(
                        new CorteEstrato(EstratoRisco.R3, 9, "Maior necessidade de apoio", "R3", 1),
                        new CorteEstrato(EstratoRisco.R2, 7, "Necessidade intermediária de apoio", "R2", 2),
                        new CorteEstrato(EstratoRisco.R1, 5, "Necessidade de apoio a acompanhar", "R1", 3),
                        new CorteEstrato(EstratoRisco.DADOS_INSUFICIENTES, null, "Completar cadastro para avaliar", "-", 4),
                        new CorteEstrato(EstratoRisco.SEM_RISCO_IDENTIFICADO, 0, "Sem prioridade indicada pela escala", "-", 5)));
    }

    /**
     * Família de teste. O padrão é "tudo informado, nada precário": dois
     * adultos com data de nascimento, banheiro com fossa séptica, água clorada
     * de cisterna, trabalho informal, 4 cômodos. Escore 0, nada indeterminado.
     */
    static final class Familia {
        Integer numeroComodos = 4;
        Boolean temBanheiro = true;
        EscoamentoSanitario escoamento = EscoamentoSanitario.FOSSA_SEPTICA;
        TratamentoAgua tratamento = TratamentoAgua.CLORADA;
        Set<AbastecimentoAgua> abastecimento = Set.of(AbastecimentoAgua.CISTERNA);
        FaixaRenda faixaRenda = FaixaRenda.ATE_1_SALARIO;
        List<TipoFonteRenda> fontes = new ArrayList<>(List.of(TipoFonteRenda.TRABALHO_INFORMAL));
        List<FatosFamilia.Pessoa> pessoas = new ArrayList<>(List.of(nascida(30), nascida(32)));

        FatosFamilia fatos() {
            return new FatosFamilia(numeroComodos, temBanheiro, escoamento, tratamento, abastecimento,
                    faixaRenda, fontes, pessoas);
        }

        Avaliacao avaliar() {
            return MotorDeInferencia.avaliar(fatos(), baseCoelhoSavassi(), HOJE);
        }
    }

    static FatosFamilia.Pessoa nascida(int anos) {
        return new FatosFamilia.Pessoa(HOJE.minusYears(anos).minusDays(10), anos);
    }

    /** Só idade estimada: sem data de nascimento. */
    static FatosFamilia.Pessoa estimada(int anos) {
        return new FatosFamilia.Pessoa(null, anos);
    }

    static ItemExplicacao item(Avaliacao avaliacao, String codigo) {
        return java.util.stream.Stream.of(avaliacao.sentinelasPresentes(), avaliacao.sentinelasAusentes(),
                        avaliacao.sentinelasIndeterminadas())
                .flatMap(List::stream)
                .filter(i -> i.codigo().equals(codigo))
                .findFirst()
                .orElseThrow();
    }

    static Set<String> codigos(List<ItemExplicacao> itens) {
        return itens.stream().map(ItemExplicacao::codigo).collect(java.util.stream.Collectors.toSet());
    }

    // ----------------------------------------------------------------- regra 1

    @Nested
    @DisplayName("dado faltando não é dado bom (regra 1)")
    class DadoFaltando {

        @Test
        @DisplayName("família sem nenhum dado → DADOS_INSUFICIENTES, nunca estrato baixo")
        void semNenhumDado() {
            FatosFamilia vazia = new FatosFamilia(null, null, null, null, null, null, null, null);

            Avaliacao avaliacao = MotorDeInferencia.avaliar(vazia, baseCoelhoSavassi(), HOJE);

            assertEquals(EstratoRisco.DADOS_INSUFICIENTES, avaliacao.estrato());
            assertNotEquals(EstratoRisco.SEM_RISCO_IDENTIFICADO, avaliacao.estrato());
            assertNull(avaliacao.escore(), "sem dado não há escore");
            assertEquals("Completar cadastro para avaliar", avaliacao.rotulo());
            assertEquals(0, avaliacao.pontosConfirmados());
            assertEquals(10, avaliacao.pontosEmAberto());
            assertTrue(avaliacao.sentinelasPresentes().isEmpty());
            assertTrue(avaliacao.sentinelasAusentes().isEmpty(), "nada pode sair 'ausente' sem dado");
            assertEquals(5, avaliacao.sentinelasIndeterminadas().size());
            assertTrue(avaliacao.camposFaltantes().containsAll(List.of(
                    "temBanheiro", "escoamentoSanitario", "tratamentoAgua", "abastecimentoAgua",
                    "faixaRenda", "fontesRenda", "pessoas", "numeroComodos")),
                    "a resposta diz o que completar: " + avaliacao.camposFaltantes());
        }

        @Test
        @DisplayName("família só com idade estimada → 'menor de seis meses' sai indeterminada, não ausente")
        void soIdadeEstimada() {
            Familia familia = new Familia();
            familia.pessoas = new ArrayList<>(List.of(estimada(30), estimada(28), estimada(3)));

            Avaliacao avaliacao = familia.avaliar();

            ItemExplicacao bebe = item(avaliacao, "MENOR_DE_SEIS_MESES");
            assertEquals(Constatacao.INDETERMINADA, bebe.constatacao());
            assertNull(bebe.pontos());
            assertEquals(List.of("pessoas.dataNascimento"), bebe.camposFaltantes());
            assertTrue(codigos(avaliacao.sentinelasIndeterminadas()).contains("MENOR_DE_SEIS_MESES"));
            assertTrue(!codigos(avaliacao.sentinelasAusentes()).contains("MENOR_DE_SEIS_MESES"));
        }

        @Test
        @DisplayName("faltar dado só leva a DADOS_INSUFICIENTES quando poderia mudar o estrato")
        void indeterminadaQueNaoMudaEstrato() {
            // tudo informado e nada precário, menos o tratamento da água: no
            // máximo somaria 3, e 3 continua abaixo de 5
            Familia familia = new Familia();
            familia.tratamento = null;

            Avaliacao avaliacao = familia.avaliar();

            assertEquals(EstratoRisco.SEM_RISCO_IDENTIFICADO, avaliacao.estrato());
            assertEquals(0, avaliacao.escore());
            assertEquals(3, avaliacao.pontosEmAberto());
            assertEquals(List.of("tratamentoAgua"), avaliacao.camposFaltantes());
        }

        @Test
        @DisplayName("falta de cômodos que pode levar de R2 a R3 → DADOS_INSUFICIENTES, com os pontos já confirmados")
        void comodosQueMudamEstrato() {
            Familia familia = new Familia();
            familia.temBanheiro = false;                                  // 3
            familia.fontes = new ArrayList<>(List.of(TipoFonteRenda.BOLSA_FAMILIA)); // 2: adultos sem trabalho
            familia.pessoas.add(nascida(75));                              // 1
            familia.pessoas.add(new FatosFamilia.Pessoa(HOJE.minusMonths(2), 0)); // 1
            familia.numeroComodos = null;                                  // 0 a 3

            Avaliacao avaliacao = familia.avaliar();

            assertEquals(EstratoRisco.DADOS_INSUFICIENTES, avaliacao.estrato());
            assertNull(avaliacao.escore());
            assertEquals(7, avaliacao.pontosConfirmados());
            assertEquals(3, avaliacao.pontosEmAberto());
            assertEquals(List.of("numeroComodos"), avaliacao.camposFaltantes());
        }

        @Test
        @DisplayName("quem já soma 9 é R3 mesmo com dado faltando: completar só pode subir")
        void r3MesmoComDadoFaltando() {
            Familia familia = new Familia();
            familia.temBanheiro = false;                                   // 3
            familia.fontes = new ArrayList<>();
            familia.faixaRenda = FaixaRenda.SEM_RENDA_FIXA;                // 2
            familia.pessoas = new ArrayList<>(List.of(nascida(30), nascida(75), estimada(9))); // 1
            familia.numeroComodos = 2;                                     // 3 (3 em 2)

            Avaliacao avaliacao = familia.avaliar();

            assertEquals(EstratoRisco.R3, avaliacao.estrato());
            assertEquals(9, avaliacao.escore());
            assertEquals(1, avaliacao.pontosEmAberto(), "menor de seis meses fica em aberto");
        }
    }

    // --------------------------------------------------------------- sentinelas

    @Nested
    @DisplayName("sentinelas")
    class Sentinelas {

        @Test
        @DisplayName("saneamento precário pontua 3 e a explicação cita o saneamento")
        void saneamentoPrecario() {
            Familia familia = new Familia();
            familia.escoamento = EscoamentoSanitario.CEU_ABERTO;

            Avaliacao avaliacao = familia.avaliar();

            assertEquals(3, avaliacao.escore());
            ItemExplicacao saneamento = item(avaliacao, "BAIXAS_CONDICOES_SANEAMENTO");
            assertEquals(Constatacao.PRESENTE, saneamento.constatacao());
            assertEquals(3, saneamento.pontos());
            assertEquals("Baixas condições de saneamento", saneamento.nome());
            assertTrue(saneamento.detalhe().contains("Céu aberto"), saneamento.detalhe());
        }

        @Test
        @DisplayName("um componente precário basta, mesmo com outros não informados")
        void umComponenteBasta() {
            Familia familia = new Familia();
            familia.tratamento = TratamentoAgua.SEM_TRATAMENTO;
            familia.temBanheiro = null;
            familia.escoamento = null;

            ItemExplicacao saneamento = item(familia.avaliar(), "BAIXAS_CONDICOES_SANEAMENTO");

            assertEquals(Constatacao.PRESENTE, saneamento.constatacao());
            assertTrue(saneamento.detalhe().contains("Sem tratamento"));
        }

        @Test
        @DisplayName("cisterna não é saneamento precário; água só de carro-pipa é")
        void cisternaNaoCarroPipaSim() {
            Familia cisterna = new Familia();
            cisterna.abastecimento = Set.of(AbastecimentoAgua.CISTERNA, AbastecimentoAgua.CARRO_PIPA);
            assertEquals(Constatacao.AUSENTE, item(cisterna.avaliar(), "BAIXAS_CONDICOES_SANEAMENTO").constatacao());

            Familia soCarroPipa = new Familia();
            soCarroPipa.abastecimento = Set.of(AbastecimentoAgua.CARRO_PIPA);
            assertEquals(Constatacao.PRESENTE, item(soCarroPipa.avaliar(), "BAIXAS_CONDICOES_SANEAMENTO").constatacao());
        }

        @ParameterizedTest(name = "{0} moradores em {1} cômodos → {2} pontos")
        @CsvSource({"4, 2, 3", "2, 2, 2", "2, 4, 0", "3, 2, 3", "1, 1, 2"})
        @DisplayName("relação morador/cômodo: maior que 1 = 3, igual a 1 = 2, menor que 1 = 0")
        void moradorPorComodo(int moradores, int comodos, int pontos) {
            Familia familia = new Familia();
            familia.pessoas = new ArrayList<>();
            for (int i = 0; i < moradores; i++) {
                familia.pessoas.add(nascida(30 + i));
            }
            familia.numeroComodos = comodos;

            Avaliacao avaliacao = familia.avaliar();

            ItemExplicacao razao = item(avaliacao, "RELACAO_MORADOR_COMODO");
            assertEquals(pontos, razao.pontos());
            assertEquals(pontos, avaliacao.escore());
            assertEquals(pontos > 0 ? Constatacao.PRESENTE : Constatacao.AUSENTE, razao.constatacao());
        }

        @Test
        @DisplayName("aposentado sozinho não é desempregado; adulto sem trabalho numa casa de aposentado é")
        void desemprego() {
            Familia aposentado = new Familia();
            aposentado.pessoas = new ArrayList<>(List.of(nascida(78)));
            aposentado.fontes = new ArrayList<>(List.of(TipoFonteRenda.APOSENTADORIA));
            assertEquals(Constatacao.AUSENTE, item(aposentado.avaliar(), "DESEMPREGO").constatacao());

            Familia comAdulto = new Familia();
            comAdulto.pessoas = new ArrayList<>(List.of(nascida(78), nascida(40)));
            comAdulto.fontes = new ArrayList<>(List.of(TipoFonteRenda.APOSENTADORIA));
            ItemExplicacao desemprego = item(comAdulto.avaliar(), "DESEMPREGO");
            assertEquals(Constatacao.PRESENTE, desemprego.constatacao());
            assertEquals(2, desemprego.pontos());
        }

        @Test
        @DisplayName("sem fonte cadastrada: faixa nula é 'não informado'; SEM_RENDA_FIXA é 'não tem'")
        void rendaNaoInformadaNaoEDesemprego() {
            Familia naoInformada = new Familia();
            naoInformada.fontes = new ArrayList<>();
            naoInformada.faixaRenda = null;
            assertEquals(Constatacao.INDETERMINADA, item(naoInformada.avaliar(), "DESEMPREGO").constatacao());

            Familia semRenda = new Familia();
            semRenda.fontes = new ArrayList<>();
            semRenda.faixaRenda = FaixaRenda.SEM_RENDA_FIXA;
            assertEquals(Constatacao.PRESENTE, item(semRenda.avaliar(), "DESEMPREGO").constatacao());
        }

        @Test
        @DisplayName("70 anos completos já é 'maior de 70'; 69 não é")
        void maiorDe70() {
            Familia setenta = new Familia();
            setenta.pessoas.add(nascida(70));
            assertEquals(Constatacao.PRESENTE, item(setenta.avaliar(), "MAIOR_DE_70_ANOS").constatacao());

            Familia sessentaENove = new Familia();
            sessentaENove.pessoas.add(nascida(69));
            assertEquals(Constatacao.AUSENTE, item(sessentaENove.avaliar(), "MAIOR_DE_70_ANOS").constatacao());
        }

        @Test
        @DisplayName("bebê com data de nascimento de 5 meses dispara; de 7 meses não")
        void menorDeSeisMeses() {
            Familia cincoMeses = new Familia();
            cincoMeses.pessoas.add(new FatosFamilia.Pessoa(HOJE.minusMonths(5), 0));
            assertEquals(Constatacao.PRESENTE, item(cincoMeses.avaliar(), "MENOR_DE_SEIS_MESES").constatacao());

            Familia seteMeses = new Familia();
            seteMeses.pessoas.add(new FatosFamilia.Pessoa(HOJE.minusMonths(7), 0));
            assertEquals(Constatacao.AUSENTE, item(seteMeses.avaliar(), "MENOR_DE_SEIS_MESES").constatacao());
        }
    }

    // ----------------------------------------------------------- estratificação

    @Nested
    @DisplayName("estratificação (Quadro 02)")
    class Estratificacao {

        @ParameterizedTest(name = "escore {0} → {1}")
        @CsvSource({
                "0, SEM_RISCO_IDENTIFICADO", "4, SEM_RISCO_IDENTIFICADO",
                "5, R1", "6, R1", "7, R2", "8, R2", "9, R3", "10, R3"})
        @DisplayName("cortes do artigo: 5 a 6 = R1, 7 a 8 = R2, 9 ou mais = R3, abaixo de 5 = sem risco identificado")
        void cortes(int escore, EstratoRisco esperado) {
            assertEquals(esperado, baseCoelhoSavassi().estratoDoEscore(escore).estrato());
        }

        @Test
        @DisplayName("famílias completas: 4 → sem risco, 6 → R1, 7 → R2, 9 → R3")
        void familiasCompletas() {
            Familia quatro = new Familia();             // saneamento 3 + idoso 1
            quatro.temBanheiro = false;
            quatro.pessoas.add(nascida(75));             // 3 em 4 cômodos: 0
            assertEstrato(quatro.avaliar(), 4, EstratoRisco.SEM_RISCO_IDENTIFICADO);

            Familia seis = new Familia();               // saneamento 3 + 3 em 2 cômodos 3
            seis.temBanheiro = false;
            seis.pessoas.add(nascida(10));
            seis.numeroComodos = 2;
            assertEstrato(seis.avaliar(), 6, EstratoRisco.R1);

            Familia sete = new Familia();               // saneamento 3 + desemprego 2 + 2 em 2 cômodos 2
            sete.temBanheiro = false;
            sete.fontes = new ArrayList<>(List.of(TipoFonteRenda.BOLSA_FAMILIA));
            sete.numeroComodos = 2;
            assertEstrato(sete.avaliar(), 7, EstratoRisco.R2);

            Familia nove = new Familia();               // saneamento 3 + desemprego 2 + 3 em 2 cômodos 3 + idoso 1
            nove.temBanheiro = false;
            nove.fontes = new ArrayList<>(List.of(TipoFonteRenda.APOSENTADORIA));
            nove.pessoas = new ArrayList<>(List.of(nascida(30), nascida(32), nascida(75)));
            nove.numeroComodos = 2;
            assertEstrato(nove.avaliar(), 9, EstratoRisco.R3);
        }

        private void assertEstrato(Avaliacao avaliacao, int escore, EstratoRisco estrato) {
            assertEquals(escore, avaliacao.escore(), () -> "explicação: " + avaliacao.sentinelasPresentes());
            assertEquals(estrato, avaliacao.estrato());
            assertTrue(avaliacao.sentinelasIndeterminadas().isEmpty());
        }

        @Test
        @DisplayName("teto alcançável: 10 com cômodos; 7 sem a sentinela de cômodos")
        void teto() {
            BaseDeConhecimento completa = baseCoelhoSavassi();
            assertEquals(10, completa.escoreMaximoAlcancavel());

            BaseDeConhecimento semComodos = new BaseDeConhecimento(
                    completa.regras().stream().filter(r -> !r.codigo().equals("RELACAO_MORADOR_COMODO")).toList(),
                    List.of(), completa.estratos());
            assertEquals(7, semComodos.escoreMaximoAlcancavel());
        }
    }

    // --------------------------------------------------------- explicação etc.

    @Test
    @DisplayName("a explicação lista exatamente as sentinelas que dispararam, e nenhuma a mais")
    void explicacaoExata() {
        Familia familia = new Familia();
        familia.escoamento = EscoamentoSanitario.CEU_ABERTO;
        familia.pessoas.add(nascida(80));

        Avaliacao avaliacao = familia.avaliar();

        assertEquals(Set.of("BAIXAS_CONDICOES_SANEAMENTO", "MAIOR_DE_70_ANOS"), codigos(avaliacao.sentinelasPresentes()));
        assertEquals(4, avaliacao.sentinelasPresentes().stream().mapToInt(ItemExplicacao::pontos).sum());
        assertEquals(avaliacao.escore(), avaliacao.sentinelasPresentes().stream().mapToInt(ItemExplicacao::pontos).sum());
        assertEquals(Set.of("DESEMPREGO", "MENOR_DE_SEIS_MESES", "RELACAO_MORADOR_COMODO"),
                codigos(avaliacao.sentinelasAusentes()));
        assertTrue(avaliacao.sentinelasIndeterminadas().isEmpty());
    }

    @Test
    @DisplayName("duas famílias idênticas recebem o mesmo escore e a mesma explicação (determinismo)")
    void determinismo() {
        Familia uma = new Familia();
        uma.temBanheiro = false;
        uma.pessoas.add(estimada(4));
        Familia outra = new Familia();
        outra.temBanheiro = false;
        outra.pessoas.add(estimada(4));

        Avaliacao primeira = uma.avaliar();
        assertEquals(primeira, outra.avaliar());
        assertEquals(primeira, uma.avaliar(), "avaliar de novo não muda nada");
    }

    @Test
    @DisplayName("o peso vem da base: mudar o peso muda o resultado sem mexer no motor")
    void pesoVemDaBase() {
        FatosFamilia fatos = new FatosFamilia(4, false, EscoamentoSanitario.FOSSA_SEPTICA, TratamentoAgua.CLORADA,
                Set.of(AbastecimentoAgua.CISTERNA), FaixaRenda.ATE_1_SALARIO,
                List.of(TipoFonteRenda.TRABALHO_FIXO), List.of(nascida(30), nascida(31)));

        assertEquals(3, MotorDeInferencia.avaliar(fatos, baseCom(3), HOJE).escore());
        Avaliacao comPesoMaior = MotorDeInferencia.avaliar(fatos, baseCom(5), HOJE);
        assertEquals(5, comPesoMaior.escore());
        assertEquals(EstratoRisco.R1, comPesoMaior.estrato());
    }

    @Test
    @DisplayName("base com parâmetro que não é valor de enum falha ao carregar, com a causa")
    void baseInvalida() {
        BaseDeConhecimento valida = baseCoelhoSavassi();
        RegraBinaria quebrada = new RegraBinaria("BAIXAS_CONDICOES_SANEAMENTO", "Baixas condições de saneamento", 3,
                new Parametros("BAIXAS_CONDICOES_SANEAMENTO", Map.of("ESCOAMENTO_PRECARIO", List.of("FOSSA_INEXISTENTE"))));

        BaseDeConhecimentoInvalidaException erro = assertThrows(BaseDeConhecimentoInvalidaException.class,
                () -> new BaseDeConhecimento(List.of(quebrada), List.of(), valida.estratos()));
        assertTrue(erro.getMessage().contains("FOSSA_INEXISTENTE"));

        // e cortes reescalados fora da ordem do instrumento também
        assertThrows(BaseDeConhecimentoInvalidaException.class, () -> new BaseDeConhecimento(valida.regras(), List.of(),
                List.of(new CorteEstrato(EstratoRisco.R3, 5, "a", "a", 1),
                        new CorteEstrato(EstratoRisco.R2, 7, "b", "b", 2),
                        new CorteEstrato(EstratoRisco.R1, 9, "c", "c", 3),
                        new CorteEstrato(EstratoRisco.DADOS_INSUFICIENTES, null, "d", "d", 4),
                        new CorteEstrato(EstratoRisco.SEM_RISCO_IDENTIFICADO, 0, "e", "e", 5))));
    }
}
