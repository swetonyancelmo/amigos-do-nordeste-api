package br.org.amigosdonordeste.cadastro.precadastro;

/**
 * Onde o pre-cadastro esta na fila de revisao da associacao (coluna
 * pre_cadastro.situacao, V9). Todo envio nasce PENDENTE; aprovar e devolver
 * sao tarefas de outras issues.
 */
public enum SituacaoPreCadastro {
    PENDENTE,
    APROVADO,
    DEVOLVIDO
}
