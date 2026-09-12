package br.org.amigosdonordeste.cadastro.dominio;

import java.time.LocalDate;
import java.time.Period;

/**
 * Idade de uma pessoa, por dois caminhos:
 *   1. data de nascimento, quando a associação sabe;
 *   2. idade estimada mais a data em que a estimativa foi feita, quando ninguém sabe.
 *
 * Devolve Integer (e não int) de propósito: null significa "não dá para saber",
 * e isso é uma resposta válida no nosso domínio.
 */
public final class Idade {

    private Idade() {}   // classe utilitária: ninguém instancia

    /**
     * Versão usada pelo sistema. Usa a data de hoje.
     */
    public static Integer calcular(LocalDate dataNascimento,
                                   Integer idadeEstimada,
                                   LocalDate idadeEstimadaEm) {
        return calcular(dataNascimento, idadeEstimada, idadeEstimadaEm, LocalDate.now());
    }

    /**
     * Versão usada pelos testes, que passam a data de "hoje" na mão.
     * É isso que faz o teste dar o mesmo resultado hoje, amanhã e no ano que vem.
     */
    static Integer calcular(LocalDate dataNascimento,
                            Integer idadeEstimada,
                            LocalDate idadeEstimadaEm,
                            LocalDate hoje) {

        // 1. A data de nascimento sempre ganha da estimativa.
        //    Se as duas existirem, a data é a informação melhor.
        if (dataNascimento != null) {
            if (dataNascimento.isAfter(hoje)) {
                return null;   // erro de digitação: nasceu no futuro
            }
            return Period.between(dataNascimento, hoje).getYears();
        }

        // 2. Sem data de nascimento, vale a estimativa — mas só se soubermos
        //    QUANDO ela foi feita. Sem isso não dá para envelhecê-la.
        if (idadeEstimada != null && idadeEstimadaEm != null) {
            if (idadeEstimadaEm.isAfter(hoje)) {
                return null;   // estimativa feita no futuro: também é erro de digitação
            }
            int anosDesdeAEstimativa = Period.between(idadeEstimadaEm, hoje).getYears();
            return idadeEstimada + anosDesdeAEstimativa;
        }

        // 3. Não tem data e não tem estimativa: a resposta honesta é "não sei".
        //    Não lançar exceção. Cadastro incompleto é normal aqui.
        return null;
    }
}
