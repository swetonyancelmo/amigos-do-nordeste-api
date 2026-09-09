package br.org.amigosdonordeste.cadastro.comunidade.enums;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Lista fechada. Valores provisorios — confirmar com a associacao e servir por
 * /api/metadados quando o endpoint de metadados existir (regra 3 do projeto).
 */


public enum TipoComunidade implements Rotulavel {

    SITIO("Sítio"),
    ASSENTAMENTO("Assentamento"),
    POVOADO("Povoado"),
    COMUNIDADE_QUILOMBOLA("Comunidade quilombola"),
    VILA("Vila"),
    DISTRITO("Distrito"),
    BAIRRO("Bairro"),
    OUTRO("Outro");

    private final String rotulo;
    TipoComunidade(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}