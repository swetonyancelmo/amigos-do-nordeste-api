package br.org.amigosdonordeste.cadastro.suporte;

import java.text.Normalizer;
import java.util.regex.Pattern;

public final class UnaccentH2 {

  private static final Pattern DIACRITICS_PATTERN =
    Pattern.compile("\\p{InCombiningDiacriticalMarks}+");

  private UnaccentH2() {
  }

  /**
   * Função estática mapeada pelo alias UNACCENT do H2.
   * Remove acentos e caracteres diacríticos idêntico ao unaccent do Postgres.
   */
  public static String unaccent(String texto) {
    if (texto == null) {
      return null;
    }
    String decomposto = Normalizer.normalize(texto, Normalizer.Form.NFD);
    return DIACRITICS_PATTERN.matcher(decomposto).replaceAll("");
  }
}
