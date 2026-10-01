package br.org.amigosdonordeste.cadastro.precadastro;

import br.org.amigosdonordeste.cadastro.dominio.Rotulavel;

/**
 * Onde o pre-cadastro esta na fila de revisao da associacao (coluna
 * pre_cadastro.situacao, V9). Todo envio nasce PENDENTE; aprovar e devolver
 * mudam a situacao, e um DEVOLVIDO reenviado pela agente volta a PENDENTE.
 * Servido em /api/metadados para o filtro da tela de Chamados.
 */
public enum SituacaoPreCadastro implements Rotulavel {
    PENDENTE("Pendente"),
    APROVADO("Aprovado"),
    DEVOLVIDO("Devolvido");

    private final String rotulo;
    SituacaoPreCadastro(String rotulo) { this.rotulo = rotulo; }
    public String getRotulo() { return rotulo; }
}
