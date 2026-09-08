package br.org.amigosdonordeste.cadastro.comunidade.enuns;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Lista fechada. Valores provisorios — confirmar com a associacao e servir por
 * /api/metadados quando o endpoint de metadados existir (regra 3 do projeto).
 */


public enum TipoComunidade implements Rotulavel {

    SITIO("Sítio"),
    ASSENTAMENTO("Assentamento"),
    DISTRITO("Distrito"),
    BAIRRO("Bairro");

    private final String rotulo;
    TipoComunidade(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}