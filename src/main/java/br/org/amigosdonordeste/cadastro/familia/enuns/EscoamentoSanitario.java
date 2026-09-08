package br.org.amigosdonordeste.cadastro.familia.enuns;

import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Destino do esgoto do domicilio. Categorias baseadas na Ficha de Cadastro
 * Domiciliar do e-SUS — nao inventamos lista (ver docs/decisoes/ADR-0003).
 * Valores provisorios, a servir por /api/metadados.
 */
public enum EscoamentoSanitario implements Rotulavel {

    FOSSA_RUDIMENTAR(124, "Fossa rudimentar"),
    FOSSA_SEPTICA(123, "Fossa séptica"),
    REDE_COLETORA(122, "Rede coletora de esgoto ou pluvial"),
    CEU_ABERTO(126, "Céu aberto"),
    DIRETO_RIO_LAGO_MAR(125, "Direto para um rio, lago ou mar"),
    OUTRA_FORMA(127, "Outra forma");

    private final int codigoESus;
    private final String rotulo;

    EscoamentoSanitario(int codigoESus, String rotulo) {
        this.codigoESus = codigoESus;
        this.rotulo = rotulo;
    }

    public int getCodigoESus() { return codigoESus; }
    public String getRotulo()  { return rotulo; }
}