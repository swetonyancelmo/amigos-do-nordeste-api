package br.org.amigosdonordeste.cadastro.precadastro;

/**
 * Por que a lista de chamados marcou um pre-cadastro como possivel duplicata
 * de uma familia ja cadastrada. Lista fechada: a tela mostra um aviso por
 * motivo. E so aviso — quem decide continua sendo a pessoa que revisa.
 */
public enum MotivoDuplicata {
    /** Mesmo telefone de uma familia da mesma comunidade. */
    TELEFONE_IGUAL,
    /** Nome da responsavel igual, ignorando acento e maiuscula, na mesma comunidade. */
    NOME_PARECIDO
}
