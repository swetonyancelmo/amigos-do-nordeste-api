package br.org.amigosdonordeste.cadastro.pessoa.enuns;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;


public enum Serie implements Rotulavel {

    PRE("PRÉ"),
    ANO_1("1º ano"),
    ANO_2("2º ano"),
    ANO_3("3º ano"),
    ANO_4("4º ano"),
    ANO_5("5º ano"),
    ANO_6("6º ano"),
    ANO_7("7º ano"),
    ANO_8("8º ano"),
    ANO_9("9º ano"),
    ENSINO_MEDIO("Ensino médio"),
    NAO_SE_APLICA("Não se aplica");

    private final String rotulo;
    Serie(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}