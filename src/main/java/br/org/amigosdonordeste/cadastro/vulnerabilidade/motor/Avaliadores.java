package br.org.amigosdonordeste.cadastro.vulnerabilidade.motor;

import br.org.amigosdonordeste.cadastro.familia.enums.AbastecimentoAgua;
import br.org.amigosdonordeste.cadastro.familia.enums.EscoamentoSanitario;
import br.org.amigosdonordeste.cadastro.familia.enums.TratamentoAgua;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.FaixaRenda;
import br.org.amigosdonordeste.cadastro.fonterenda.enums.TipoFonteRenda;
import br.org.amigosdonordeste.cadastro.vulnerabilidade.motor.FatosFamilia.Pessoa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Como cada sentinela LÊ a família. É a única parte do sistema especialista
 * que é código, porque traduz o cadastro (enums do e-SUS, pessoas, renda) em
 * fato. Pesos, faixas, cortes e os valores que contam como "precário" não
 * estão aqui: vêm da base (Parametros). Critérios explicados na ADR-0010.
 *
 * Regra de ouro (ADR-0010, regra 1): campo nulo nunca é "não tem". Se o dado
 * que decidiria falta, a leitura é INDETERMINADA e diz qual campo falta.
 */
final class Avaliadores {

    private Avaliadores() { }

    /** Leitura de sentinela binária. */
    interface Binario {
        /** Converte os parâmetros, para uma base quebrada falhar ao carregar e não no meio da lista. */
        void validar(Parametros parametros);

        Leitura avaliar(FatosFamilia fatos, Parametros parametros, LocalDate hoje);
    }

    /** Medida de sentinela de faixa: numerador/denominador, ou indeterminada. */
    interface DeFaixa {
        Medida medir(FatosFamilia fatos);
    }

    record Leitura(Constatacao constatacao, String detalhe, List<String> faltando) {
        static Leitura presente(String detalhe) {
            return new Leitura(Constatacao.PRESENTE, detalhe, List.of());
        }

        static Leitura ausente(String detalhe) {
            return new Leitura(Constatacao.AUSENTE, detalhe, List.of());
        }

        static Leitura indeterminada(String detalhe, List<String> faltando) {
            return new Leitura(Constatacao.INDETERMINADA, detalhe, List.copyOf(faltando));
        }
    }

    /** numerador e denominador nulos = indeterminada (faltando diz o quê). */
    record Medida(Integer numerador, Integer denominador, String detalhe, List<String> faltando) {
        boolean indeterminada() {
            return numerador == null || denominador == null;
        }
    }

    // Nomes de campo devolvidos em camposFaltantes: os mesmos do JSON da
    // família, para o web saber qual campo do formulário destacar.
    static final String CAMPO_PESSOAS = "pessoas";
    static final String CAMPO_IDADE = "pessoas.idade";
    static final String CAMPO_DATA_NASCIMENTO = "pessoas.dataNascimento";

    private static final Map<String, Binario> BINARIOS = Map.of(
            "BAIXAS_CONDICOES_SANEAMENTO", new Saneamento(),
            "DESEMPREGO", new Desemprego(),
            "MAIOR_DE_70_ANOS", new MaiorDeIdade(),
            "MENOR_DE_SEIS_MESES", new MenorDeMeses());

    private static final Map<String, DeFaixa> DE_FAIXA = Map.of(
            "RELACAO_MORADOR_COMODO", new MoradorPorComodo());

    static Binario binario(String codigo) {
        Binario avaliador = BINARIOS.get(codigo);
        if (avaliador == null) {
            throw new BaseDeConhecimentoInvalidaException("sentinela binária \"" + codigo
                    + "\" está AVALIADA, mas o sistema não sabe lê-la na família");
        }
        return avaliador;
    }

    static DeFaixa deFaixa(String codigo) {
        DeFaixa avaliador = DE_FAIXA.get(codigo);
        if (avaliador == null) {
            throw new BaseDeConhecimentoInvalidaException("sentinela de faixa \"" + codigo
                    + "\" está AVALIADA, mas o sistema não sabe medi-la na família");
        }
        return avaliador;
    }

    // ------------------------------------------------------------------ saneamento

    /**
     * Baixas condições de saneamento: basta UM componente precário (sem
     * banheiro, escoamento precário, água de beber sem tratamento ou água só
     * de fonte precária). Para dizer AUSENTE, todos os componentes com critério
     * ligado têm que estar informados. Um critério sem valores na base fica
     * desligado e não é exigido.
     */
    static final class Saneamento implements Binario {

        @Override
        public void validar(Parametros p) {
            p.enums("ESCOAMENTO_PRECARIO", EscoamentoSanitario.class);
            p.enums("TRATAMENTO_PRECARIO", TratamentoAgua.class);
            p.enums("ABASTECIMENTO_SO_DE", AbastecimentoAgua.class);
        }

        @Override
        public Leitura avaliar(FatosFamilia f, Parametros p, LocalDate hoje) {
            Set<EscoamentoSanitario> escoamentoPrecario = p.enums("ESCOAMENTO_PRECARIO", EscoamentoSanitario.class);
            Set<TratamentoAgua> tratamentoPrecario = p.enums("TRATAMENTO_PRECARIO", TratamentoAgua.class);
            Set<AbastecimentoAgua> abastecimentoPrecario = p.enums("ABASTECIMENTO_SO_DE", AbastecimentoAgua.class);

            List<String> motivos = new ArrayList<>();
            List<String> faltando = new ArrayList<>();

            if (Boolean.FALSE.equals(f.temBanheiro())) {
                motivos.add("sem banheiro");
            } else if (f.temBanheiro() == null) {
                faltando.add("temBanheiro");
            }

            if (!escoamentoPrecario.isEmpty()) {
                if (f.escoamentoSanitario() == null) {
                    faltando.add("escoamentoSanitario");
                } else if (escoamentoPrecario.contains(f.escoamentoSanitario())) {
                    motivos.add("escoamento: " + f.escoamentoSanitario().getRotulo());
                }
            }

            if (!tratamentoPrecario.isEmpty()) {
                if (f.tratamentoAgua() == null) {
                    faltando.add("tratamentoAgua");
                } else if (tratamentoPrecario.contains(f.tratamentoAgua())) {
                    motivos.add("água de beber: " + f.tratamentoAgua().getRotulo());
                }
            }

            if (!abastecimentoPrecario.isEmpty()) {
                if (f.abastecimentoAgua().isEmpty()) {
                    faltando.add("abastecimentoAgua");
                } else if (abastecimentoPrecario.containsAll(f.abastecimentoAgua())) {
                    motivos.add("água só de: " + f.abastecimentoAgua().stream()
                            .map(AbastecimentoAgua::getRotulo).sorted().collect(Collectors.joining(", ")));
                }
            }

            if (!motivos.isEmpty()) {
                return Leitura.presente(String.join("; ", motivos));
            }
            if (!faltando.isEmpty()) {
                return Leitura.indeterminada("nenhum componente informado é precário, mas faltam dados de moradia",
                        faltando);
            }
            return Leitura.ausente("banheiro, escoamento, tratamento e abastecimento informados; nenhum precário");
        }
    }

    // ------------------------------------------------------------------ desemprego

    /**
     * Desemprego (proxy declarado na ADR-0010): nenhuma fonte de renda de
     * trabalho E ao menos uma pessoa em idade ativa. Aposentado sozinho não
     * é desempregado. A faixa de renda NÃO decide: só serve para saber se
     * "nenhuma fonte cadastrada" quer dizer "não tem renda" (SEM_RENDA_FIXA)
     * ou "ninguém preencheu" (faixa nula).
     */
    static final class Desemprego implements Binario {

        @Override
        public void validar(Parametros p) {
            p.enums("FONTE_DE_TRABALHO", TipoFonteRenda.class);
            p.inteiro("IDADE_ATIVA_MINIMA");
            p.inteiro("IDADE_ATIVA_MAXIMA");
        }

        @Override
        public Leitura avaliar(FatosFamilia f, Parametros p, LocalDate hoje) {
            Set<TipoFonteRenda> trabalho = p.enums("FONTE_DE_TRABALHO", TipoFonteRenda.class);
            int minima = p.inteiro("IDADE_ATIVA_MINIMA");
            int maxima = p.inteiro("IDADE_ATIVA_MAXIMA");

            if (f.fontesRenda().stream().anyMatch(trabalho::contains)) {
                return Leitura.ausente("há fonte de renda de trabalho");
            }

            if (f.fontesRenda().isEmpty()) {
                if (f.faixaRenda() == null) {
                    return Leitura.indeterminada("renda não informada", List.of("faixaRenda", "fontesRenda"));
                }
                if (f.faixaRenda() != FaixaRenda.SEM_RENDA_FIXA) {
                    return Leitura.indeterminada("há renda, mas não se sabe de onde vem", List.of("fontesRenda"));
                }
            }

            long emIdadeAtiva = f.pessoas().stream()
                    .filter(pessoa -> pessoa.idade() != null)
                    .filter(pessoa -> pessoa.idade() >= minima && pessoa.idade() <= maxima)
                    .count();
            String semTrabalho = f.fontesRenda().isEmpty() ? "sem renda fixa" : "nenhuma renda de trabalho";

            if (emIdadeAtiva > 0) {
                return Leitura.presente(semTrabalho + " e " + pessoas(emIdadeAtiva)
                        + " de " + minima + " a " + maxima + " anos");
            }
            if (f.pessoas().isEmpty()) {
                return Leitura.indeterminada(semTrabalho + "; nenhum morador cadastrado", List.of(CAMPO_PESSOAS));
            }
            if (f.pessoas().stream().anyMatch(pessoa -> pessoa.idade() == null)) {
                return Leitura.indeterminada(semTrabalho + "; há morador sem idade", List.of(CAMPO_IDADE));
            }
            return Leitura.ausente(semTrabalho + ", mas ninguém de " + minima + " a " + maxima + " anos");
        }
    }

    // ------------------------------------------------------------- maior de 70 anos

    /**
     * Maior de 70 anos: idade em anos completos >= 70. Quem completou 70 já
     * viveu mais de 70 anos; exigir 71 deixaria de fora quase um ano inteiro
     * de quem o artigo descreve.
     */
    static final class MaiorDeIdade implements Binario {

        @Override
        public void validar(Parametros p) {
            p.inteiro("IDADE_MINIMA");
        }

        @Override
        public Leitura avaliar(FatosFamilia f, Parametros p, LocalDate hoje) {
            int minima = p.inteiro("IDADE_MINIMA");
            if (f.pessoas().isEmpty()) {
                return Leitura.indeterminada("nenhum morador cadastrado", List.of(CAMPO_PESSOAS));
            }
            long idosos = f.pessoas().stream()
                    .filter(pessoa -> pessoa.idade() != null && pessoa.idade() >= minima)
                    .count();
            if (idosos > 0) {
                return Leitura.presente(pessoas(idosos) + " com " + minima + " anos ou mais");
            }
            long semIdade = f.pessoas().stream().filter(pessoa -> pessoa.idade() == null).count();
            if (semIdade > 0) {
                return Leitura.indeterminada(pessoas(semIdade) + " sem idade", List.of(CAMPO_IDADE));
            }
            return Leitura.ausente("ninguém com " + minima + " anos ou mais");
        }
    }

    // ------------------------------------------------------------ menor de 6 meses

    /**
     * Menor de seis meses: só a data de nascimento enxerga meses. Pessoa só
     * com idade estimada (em anos) deixa a sentinela INDETERMINADA, mesmo que
     * a estimativa seja de um adulto: é a regra combinada (ADR-0010).
     * Data de nascimento no futuro é erro de digitação e conta como ausente
     * de data.
     */
    static final class MenorDeMeses implements Binario {

        @Override
        public void validar(Parametros p) {
            p.inteiro("MESES");
        }

        @Override
        public Leitura avaliar(FatosFamilia f, Parametros p, LocalDate hoje) {
            int meses = p.inteiro("MESES");
            if (f.pessoas().isEmpty()) {
                return Leitura.indeterminada("nenhum morador cadastrado", List.of(CAMPO_PESSOAS));
            }
            long bebes = f.pessoas().stream()
                    .map(Pessoa::dataNascimento)
                    .filter(data -> data != null && !data.isAfter(hoje) && data.plusMonths(meses).isAfter(hoje))
                    .count();
            if (bebes > 0) {
                return Leitura.presente(pessoas(bebes) + " com menos de " + meses + " meses");
            }
            long semData = f.pessoas().stream()
                    .map(Pessoa::dataNascimento)
                    .filter(data -> data == null || data.isAfter(hoje))
                    .count();
            if (semData > 0) {
                return Leitura.indeterminada(pessoas(semData)
                        + " sem data de nascimento (idade estimada é em anos e não alcança meses)",
                        List.of(CAMPO_DATA_NASCIMENTO));
            }
            return Leitura.ausente("ninguém com menos de " + meses + " meses");
        }
    }

    // ------------------------------------------------------- morador por cômodo

    /** Moradores = pessoas da família (regra 2: contagem, nunca coluna). */
    static final class MoradorPorComodo implements DeFaixa {

        @Override
        public Medida medir(FatosFamilia f) {
            List<String> faltando = new ArrayList<>();
            if (f.pessoas().isEmpty()) {
                faltando.add(CAMPO_PESSOAS);
            }
            if (f.numeroComodos() == null) {
                faltando.add("numeroComodos");
            }
            if (!faltando.isEmpty()) {
                return new Medida(null, null, "número de cômodos ou de moradores não informado", faltando);
            }
            int moradores = f.pessoas().size();
            int comodos = f.numeroComodos();
            return new Medida(moradores, comodos,
                    moradores + (moradores == 1 ? " morador" : " moradores") + " em "
                            + comodos + (comodos == 1 ? " cômodo" : " cômodos"),
                    List.of());
        }
    }

    private static String pessoas(long quantidade) {
        return quantidade + (quantidade == 1 ? " pessoa" : " pessoas");
    }
}
