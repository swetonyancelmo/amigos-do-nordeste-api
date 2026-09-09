package br.org.amigosdonordeste.cadastro.pessoa.enums;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;
/** Lista fechada. A servir por /api/metadados quando o endpoint existir. */
public enum Sexo implements Rotulavel {

    FEMININO("Feminino"),
    MASCULINO("Masculino");

    private final String rotulo;

    Sexo(String rotulo) {
        this.rotulo = rotulo;
    }

    @Override
    public String getRotulo() {
        return rotulo;
    }
}