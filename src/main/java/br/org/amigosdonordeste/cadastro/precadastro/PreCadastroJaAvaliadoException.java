package br.org.amigosdonordeste.cadastro.precadastro;

/**
 * Aprovar ou devolver um pre-cadastro que ja saiu de PENDENTE. Vira 409: e o
 * que impede o segundo clique em "aprovar" de criar a segunda familia.
 */
public class PreCadastroJaAvaliadoException extends RuntimeException {
    public PreCadastroJaAvaliadoException(SituacaoPreCadastro situacao) {
        super("Este pré-cadastro já foi avaliado (situação " + situacao + ").");
    }
}
