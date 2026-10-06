package br.org.amigosdonordeste.cadastro.comum;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;

/**
 * O "contém" das buscas por nome (famílias e pessoas). O cadastro vem de
 * papel, então a comparação ignora:
 *   - maiúscula e acento: "Jose" acha "José" (unaccent, extensão da V1; no H2
 *     dos testes, o alias UNACCENT de application-test.yml);
 *   - apóstrofo: "davila" acha "D'Ávila", e "d'avila" também.
 * Vale para os dois lados: o nome gravado e o que foi digitado.
 */
public final class BuscaPorNome {

    private static final char ESCAPE = '\\';

    // reto, tipográficos e acentos soltos que aparecem no lugar do apóstrofo
    private static final String[] APOSTROFOS = {"'", "’", "‘", "`", "´"};

    private BuscaPorNome() { }

    public static Predicate contem(CriteriaBuilder cb, Expression<String> coluna, String digitado) {
        String termo = "%" + escaparCuringas(semApostrofo(digitado.trim())) + "%";
        Expression<String> nome = cb.function("unaccent", String.class, cb.lower(coluna));
        for (String apostrofo : APOSTROFOS) {
            nome = cb.function("replace", String.class, nome, cb.literal(apostrofo), cb.literal(""));
        }
        Expression<String> busca = cb.function("unaccent", String.class, cb.lower(cb.literal(termo)));
        return cb.like(nome, busca, ESCAPE);
    }

    private static String semApostrofo(String texto) {
        for (String apostrofo : APOSTROFOS) {
            texto = texto.replace(apostrofo, "");
        }
        return texto;
    }

    /** "%" e "_" digitados na busca são texto, não curinga do LIKE. */
    private static String escaparCuringas(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
