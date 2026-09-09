package br.org.amigosdonordeste.cadastro.familia.enums;

import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * De onde vem a agua da familia. Multipla escolha: cisterna e carro-pipa
 * convivem, e e o caso comum no sertao — por isso vira tabela auxiliar, nunca
 * coluna. Ver docs/decisoes/ADR-0003.
 *
 * Categorias baseadas na Ficha de Cadastro Domiciliar do e-SUS. Valores
 * provisorios — confirmar e servir por /api/metadados.
 */
public enum AbastecimentoAgua implements Rotulavel {

    REDE_PUBLICA(117, "Rede encanada até o domicílio"),
    POCO_NASCENTE_NO_DOMICILIO(118, "Poço ou nascente no domicílio"),
    CISTERNA(119, "Cisterna — água de chuva"),
    CARRO_PIPA(120, "Carro-pipa"),
    CAPTACAO_DIRETA_RIO(215, "Captação direta de água do rio"),
    POCO_COLETIVO(216, "Captação direta de poço coletivo"),
    CHAFARIZ(217, "Ponto de abastecimento coletivo — chafariz"),
    OUTRO(121, "Outro");

    private final int codigoESus;
    private final String rotulo;

    AbastecimentoAgua(int codigoESus, String rotulo) {
        this.codigoESus = codigoESus;
        this.rotulo = rotulo;
    }

    public int getCodigoESus() { return codigoESus; }
    public String getRotulo()  { return rotulo; }
}