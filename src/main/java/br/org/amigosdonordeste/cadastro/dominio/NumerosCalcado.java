package br.org.amigosdonordeste.cadastro.dominio;

import java.util.List;


/**NumerosCalcado:
 * Classe que mantém a lista fixa de números de calçado utilizados pela
 * associação. Não é um enum porque os valores possuem o formato "26/27",
 * "34/35", etc., e são armazenados como texto.
 */

/** Escala de calçado no formato que a associação já usa no papel. */
public final class NumerosCalcado {

    public static final List<String> VALORES = List.of(
        "16/17", "18/19", "20/21", "22/23", "24/25", "26/27", "28/29",
        "30/31", "32/33", "34/35", "36/37", "38/39", "40/41", "42/43", "43/44"
    );

    public static boolean ehValido(String numero) {
        return numero != null && VALORES.contains(numero);
    }

    private NumerosCalcado() {}
}