package br.org.amigosdonordeste.cadastro.pessoa.enums;
import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Tamanho de roupa do membro. Alimenta a contagem de roupa por comunidade
 * (RF-03) — e contagem, nunca coluna de total.
 *
 * Lista fechada, valores provisorios — a associacao usa faixas propria nos
 * documentos atuais; confirmar e servir por /api/metadados.
 */
public enum TamanhoRoupa implements Rotulavel {

    RN("RN"),
    BEBE_P("Bebê P"),
    BEBE_M("Bebê M"),
    BEBE_G("Bebê G"),
    INFANTIL_2("Infantil 2"),
    INFANTIL_4("Infantil 4"),
    INFANTIL_6("Infantil 6"),
    INFANTIL_8("Infantil 8"),
    INFANTIL_10("Infantil 10"),
    INFANTIL_12("Infantil 12"),
    INFANTIL_14("Infantil 14"),
    ADULTO_PP("Adulto PP"),
    ADULTO_P("Adulto P"),
    ADULTO_M("Adulto M"),
    ADULTO_G("Adulto G"),
    ADULTO_GG("Adulto GG"),
    ADULTO_XG("Adulto XG"),
    ADULTO_XGG("Adulto XGG");

    private final String rotulo;
    TamanhoRoupa(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
