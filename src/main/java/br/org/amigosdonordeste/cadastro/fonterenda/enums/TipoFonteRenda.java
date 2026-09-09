package br.org.amigosdonordeste.cadastro.fonterenda.enums;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
/**
 * Tipo da fonte de renda. Lista fechada; a associacao citou bolsa familia,
 * aposentadoria, BPC e trabalho sazonal (ver docs/decisoes/ADR-0003). Valores
 * provisorios, a servir por /api/metadados.
 */
public enum TipoFonteRenda implements Rotulavel {

    BOLSA_FAMILIA("Bolsa Família",     true),
    APOSENTADORIA("Aposentadoria",     false),
    BPC("BPC",                         false),
    TRABALHO_SAZONAL("Trabalho sazonal", false),
    TRABALHO_FIXO("Trabalho fixo",     false),
    TRABALHO_INFORMAL("Trabalho informal", false),
    AUXILIO_DOENCA("Auxílio-doença",   false),
    PENSAO("Pensão",                   false),
    NENHUMA("Nenhuma",                 true),
    OUTRA("Outra",                     true);

    private final String rotulo;
    private final boolean daFamilia;

    TipoFonteRenda(String rotulo, boolean daFamilia) {
        this.rotulo = rotulo;
        this.daFamilia = daFamilia;
    }

    public String getRotulo() { return rotulo; }

    /** true quando o benefício é da família e não faz sentido perguntar quem recebe. */
    public boolean ehDaFamilia() { return daFamilia; }
}
