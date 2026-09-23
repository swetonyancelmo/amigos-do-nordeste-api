package br.org.amigosdonordeste.cadastro.suporte;

import java.text.Normalizer;

/**
 * Substituto da extensao unaccent do Postgres para o H2 dos testes. E
 * registrado como alias UNACCENT pelo INIT da URL em application-test.yml,
 * para as consultas nativas de duplicata rodarem com o mesmo SQL de producao.
 */
public final class UnaccentH2 {

    private UnaccentH2() { }

    public static String unaccent(String texto) {
        if (texto == null) {
            return null;
        }
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
