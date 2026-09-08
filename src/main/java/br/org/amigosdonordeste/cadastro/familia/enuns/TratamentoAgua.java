package br.org.amigosdonordeste.cadastro.familia.enuns;

/**
 * Como a familia trata a agua de beber. Categorias baseadas na Ficha de
 * Cadastro Domiciliar do e-SUS (ver docs/decisoes/ADR-0003). Valores
 * provisorios, a servir por /api/metadados.
 */
public enum TratamentoAgua {

    SEM_TRATAMENTO(100, "Sem tratamento"),
    FILTRADA_FILTRO_BARRO(218, "Filtrada com filtro de barro"),
    FILTRADA_OUTRO_FILTRO(219, "Filtrada por outro tipo de filtro"),
    CLORADA(99, "Clorada"),
    CLORADA_HIPOCLORITO(220, "Clorada com hipoclorito de sódio"),
    FERVIDA(98, "Fervida"),
    MINERAL(152, "Mineral");

    private final int codigoESus;
    private final String rotulo;

    TratamentoAgua(int codigoESus, String rotulo) {
        this.codigoESus = codigoESus;
        this.rotulo = rotulo;
    }

    public int getCodigoESus() { return codigoESus; }
    public String getRotulo()  { return rotulo; }
}