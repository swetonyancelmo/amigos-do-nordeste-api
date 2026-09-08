package br.org.amigosdonordeste.cadastro.pessoa.enuns;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;


public enum FaixaEtaria implements Rotulavel {

    ATE_12("Até 12 anos"),
    DE_13_A_59("De 13 a 59 anos"),
    DE_60_OU_MAIS("60 anos ou mais");

    private final String rotulo;
    FaixaEtaria(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }

    /** Classifica uma idade já calculada. Devolve null quando não há idade. */
    public static FaixaEtaria de(Integer idade) {
        if (idade == null)  return null;
        if (idade <= 12)    return ATE_12;
        if (idade <= 59)    return DE_13_A_59;
        return DE_60_OU_MAIS;
    }
}